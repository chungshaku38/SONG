/*
 * Copyright (c) 2018. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.icgc.dcc.song.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.dcc.song.core.utils.JsonUtils;
import org.icgc.dcc.song.server.model.Upload;
import org.icgc.dcc.song.server.model.analysis.Analysis;
import org.icgc.dcc.song.server.repository.UploadRepository;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.icgc.dcc.song.core.exceptions.ServerErrors.ANALYSIS_ID_NOT_CREATED;
import static org.icgc.dcc.song.core.exceptions.ServerErrors.PAYLOAD_PARSING;
import static org.icgc.dcc.song.core.exceptions.ServerErrors.UPLOAD_ID_NOT_FOUND;
import static org.icgc.dcc.song.core.exceptions.ServerErrors.UPLOAD_ID_NOT_VALIDATED;
import static org.icgc.dcc.song.core.exceptions.ServerErrors.UPLOAD_REPOSITORY_CREATE_RECORD;
import static org.icgc.dcc.song.core.exceptions.ServerException.buildServerException;
import static org.icgc.dcc.song.core.exceptions.ServerException.checkServer;
import static org.icgc.dcc.song.core.utils.JsonUtils.fromSingleQuoted;
import static org.icgc.dcc.song.server.model.enums.IdPrefix.UPLOAD_PREFIX;
import static org.icgc.dcc.song.server.model.enums.UploadStates.CREATED;
import static org.icgc.dcc.song.server.model.enums.UploadStates.SAVED;
import static org.icgc.dcc.song.server.model.enums.UploadStates.UPDATED;
import static org.icgc.dcc.song.server.model.enums.UploadStates.VALIDATED;
import static org.icgc.dcc.song.server.model.enums.UploadStates.resolveState;
import static org.springframework.http.ResponseEntity.ok;

@RequiredArgsConstructor
@Service
@Slf4j
public class UploadService {

  @Autowired
  private final IdService id;
  @Autowired
  private final ValidationService validator;
  @Autowired
  private final AnalysisService analysisService;
  @Autowired
  private final UploadRepository uploadRepository;

  @Autowired
  private final StudyService studyService;

  public Upload read(@NonNull String uploadId) {
    val upload = uploadRepository.get(uploadId);
    checkServer(!isNull(upload), this.getClass(), UPLOAD_ID_NOT_FOUND,
          "The uploadId '%s' was not found", uploadId);
    return upload;
  }

  private void create(@NonNull String studyId, String analysisId, @NonNull String uploadId,
                      @NonNull String jsonPayload) {
    uploadRepository.create(uploadId, studyId, analysisId, CREATED.getText(), jsonPayload);
  }

  private void update(@NonNull String uploadId, @NonNull String jsonPayload) {
    uploadRepository.update_payload(uploadId, UPDATED.getText(), jsonPayload);
  }

  @SneakyThrows
  public ResponseEntity<String> upload(@NonNull String studyId, @NonNull String payload, boolean isAsyncValidation) {
    studyService.checkStudyExist(studyId);
    String analysisType;
    String uploadId;
    val status = JsonUtils.ObjectNode();
    status.put("status","ok");

    try {
      val analysisId=JsonUtils.readTree(payload).at("/analysisId").asText();
      List<String> ids;

      if (isNullOrEmpty(analysisId)) {
        // Our business rules say that we always want to create a new record if no analysisId is set,
        // even if the rest of the content is duplicated.
        ids = Collections.emptyList();
      } else {
        ids = uploadRepository.findByBusinessKey(studyId, analysisId);
      }

      if (isNull(ids) || ids.isEmpty()) {
        uploadId = id.generate(UPLOAD_PREFIX);
        create(studyId, analysisId, uploadId, payload);
      } else if (ids.size() == 1) {
        uploadId = ids.get(0);
        val previousUpload = uploadRepository.get(uploadId);
        status.put("status",
                format("WARNING: replaced content for analysisId '%s'",
                        analysisId));
        status.put("replaced", previousUpload.getPayload());
        update(uploadId, payload);

      } else {
        throw buildServerException(getClass(), UPLOAD_ID_NOT_FOUND,
                "Multiple upload ids found for analysisId='%s', study='%s'",
                analysisId, studyId);
      }
      analysisType = JsonUtils.readTree(payload).at("/analysisType").asText("");
    } catch (UnableToExecuteStatementException jdbie) {
      log.error(jdbie.getCause().getMessage());
      throw buildServerException( getClass(), UPLOAD_REPOSITORY_CREATE_RECORD,
          "Unable to create record in upload repository");
    } catch (JsonProcessingException jpe){
      log.error(jpe.getCause().getMessage());
      throw buildServerException(getClass(), PAYLOAD_PARSING,
          "Unable parse the input payload: %s ",payload);
    }

    if (isAsyncValidation){
      validator.asyncValidate(uploadId, payload, analysisType); // Asynchronous operation.
    } else {
      validator.syncValidate(uploadId, payload, analysisType); // Synchronous operation
    }
    status.put("uploadId", uploadId);
    return ok(status.toString());
  }


  public ResponseEntity<String> save(@NonNull String studyId, @NonNull String uploadId,
      final boolean ignoreAnalysisIdCollisions) {
    studyService.checkStudyExist(studyId);
    val upload = read(uploadId);
    checkServer(!isNull(upload),this.getClass(), UPLOAD_ID_NOT_FOUND,
          "UploadId %s does not exist", uploadId);
    val uploadState = resolveState(upload.getState());

    checkServer(uploadState == SAVED || uploadState == VALIDATED, this.getClass(),
        UPLOAD_ID_NOT_VALIDATED,
        "UploadId %s is in state '%s', but must be in state '%s' before it can be saved",
        uploadId, uploadState.getText(), VALIDATED.getText());
    val json = upload.getPayload();
    val analysis = JsonUtils.fromJson(json, Analysis.class);
    val analysisId = analysisService.create(studyId, analysis, ignoreAnalysisIdCollisions);
    checkServer(!isNull(analysisId),this.getClass(), ANALYSIS_ID_NOT_CREATED,
        "Could not create analysisId for upload id '%s",uploadId);
    updateAsSaved(uploadId);
    val reply = fromSingleQuoted(format("{'analysisId': '%s', 'status': '%s'}", analysisId, "ok"));
    return ok(reply);
  }

  private void updateAsSaved(@NonNull String uploadId) {
    uploadRepository.update(uploadId, SAVED.getText(), "");
  }

}
