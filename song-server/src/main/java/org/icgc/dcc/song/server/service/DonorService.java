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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.icgc.dcc.song.server.model.entity.Donor;
import org.icgc.dcc.song.server.model.entity.composites.DonorWithSpecimens;
import org.icgc.dcc.song.server.repository.DonorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.isNull;
import static org.icgc.dcc.song.core.exceptions.ServerErrors.DONOR_ALREADY_EXISTS;
import static org.icgc.dcc.song.core.exceptions.ServerErrors.DONOR_DOES_NOT_EXIST;
import static org.icgc.dcc.song.core.exceptions.ServerErrors.DONOR_ID_IS_CORRUPTED;
import static org.icgc.dcc.song.core.exceptions.ServerErrors.DONOR_RECORD_FAILED;
import static org.icgc.dcc.song.core.exceptions.ServerErrors.DONOR_REPOSITORY_DELETE_RECORD;
import static org.icgc.dcc.song.core.exceptions.ServerErrors.DONOR_REPOSITORY_UPDATE_RECORD;
import static org.icgc.dcc.song.core.exceptions.ServerException.checkServer;
import static org.icgc.dcc.song.core.utils.Responses.OK;

@RequiredArgsConstructor
@Service
public class DonorService {

  @Autowired
  private final DonorRepository donorRepository;
  @Autowired
  private final DonorInfoService infoService;
  @Autowired
  private final IdService idService;
  @Autowired
  private final SpecimenService specimenService;
  @Autowired
  private final StudyService studyService;

  private String createDonorId(DonorWithSpecimens donorWithSpecimens){
    studyService.checkStudyExist(donorWithSpecimens.getStudyId());
    val inputDonorId = donorWithSpecimens.getDonorId();
    val id = idService.generateDonorId(donorWithSpecimens.getDonorSubmitterId(), donorWithSpecimens.getStudyId());
    checkServer(isNullOrEmpty(inputDonorId) || id.equals(inputDonorId), getClass(),
        DONOR_ID_IS_CORRUPTED,
        "The input donorId '%s' is corrupted because it does not match the idServices donorId '%s'",
        inputDonorId, id);
    checkDonorDoesNotExist(id);
    return id;
  }

  public String create(@NonNull DonorWithSpecimens donorWithSpecimens) {
    val id = createDonorId(donorWithSpecimens);
    donorWithSpecimens.setDonorId(id);

    /**
     * Cannot pass DonorWithSpecimen object to donorReposityr.create() method
     */
    val donor = donorWithSpecimens.createDonor();
    val status = donorRepository.create(donor);
    checkServer(status == 1, this.getClass(),
        DONOR_RECORD_FAILED, "Cannot create Donor: %s", donorWithSpecimens.toString());
    infoService.create(id, donor.getInfoAsString());
    donorWithSpecimens.getSpecimens().forEach(s -> specimenService.create(donorWithSpecimens.getStudyId(), s));
    return id;
  }

  public Donor read(@NonNull String id) {
    val donor = donorRepository.read(id);
    checkServer(!isNull(donor), getClass(), DONOR_DOES_NOT_EXIST,
      "The donor for donorId '%s' could not be read because it does not exist", id);
    donor.setInfo(infoService.readNullableInfo(id));
    return donor;
  }

  public DonorWithSpecimens readWithSpecimens(@NonNull String id) {
    val donor = new DonorWithSpecimens();
    donor.setDonor(read(id));

    donor.setSpecimens(specimenService.readByParentId(id));
    return donor;
  }

  public List<DonorWithSpecimens> readByParentId(@NonNull String parentId) {
    studyService.checkStudyExist(parentId);
    val donors = new ArrayList<DonorWithSpecimens>();
    val ids = donorRepository.findByParentId(parentId);
    ids.forEach(id -> donors.add(readWithSpecimens(id)));

    return donors;
  }

  public boolean isDonorExist(@NonNull String id){
    return !isNull(donorRepository.read(id));
  }

  public void checkDonorExists(@NonNull Donor donor){
    checkDonorExists(donor.getDonorId());
  }

  public void checkDonorExists(@NonNull String id){
    checkServer(isDonorExist(id), this.getClass(), DONOR_DOES_NOT_EXIST,
        "The donor with donorId '%s' does not exist", id);
  }

  public void checkDonorDoesNotExist(@NonNull String id){
    checkServer(!isDonorExist(id), getClass(), DONOR_ALREADY_EXISTS,
        "The donor with donorId '%s' already exists", id);
  }

  public String update(@NonNull Donor donor) {
    checkDonorExists(donor);
    val status = donorRepository.update(donor);
    checkServer(status == 1, getClass(), DONOR_REPOSITORY_UPDATE_RECORD,
        "Cannot update donorId '%s' for donor '%s'", donor.getDonorId(), donor);
    infoService.update(donor.getDonorId(), donor.getInfoAsString());
    return OK;
  }

  public String delete(@NonNull String studyId, @NonNull List<String> ids) {
    studyService.checkStudyExist(studyId);
    ids.forEach(x -> internalDelete(studyId, x));
    return OK;
  }

  public String delete(@NonNull String studyId, @NonNull String id) {
    studyService.checkStudyExist(studyId);
    return internalDelete(studyId, id);
  }

  private String internalDelete(String studyId, String id){
    checkDonorExists(id);
    specimenService.deleteByParentId(id);
    val status = donorRepository.delete(studyId, id);
    checkServer(status == 1, getClass(), DONOR_REPOSITORY_DELETE_RECORD,
        "Cannot delete donor with donorId '%s'", id);
    infoService.delete(id);
    return OK;
  }

  // TODO: [SONG-254] DeleteByParentId spec missing -- https://github.com/overture-stack/SONG/issues/254
  public String deleteByParentId(@NonNull String studyId) {
    studyService.checkStudyExist(studyId);
    donorRepository.findByParentId(studyId).forEach(id -> internalDelete(studyId, id));
    return OK;
  }

  public String save(@NonNull String studyId, @NonNull Donor donor) {
    donor.setStudyId(studyId);

    String donorId = donorRepository.findByBusinessKey(studyId, donor.getDonorSubmitterId());
    val donorWithSpecimens = new DonorWithSpecimens();
    donorWithSpecimens.setDonor(donor);
    donorWithSpecimens.setDonorId(donorId);
    if (isNull(donorId)) {
      donorId = create(donorWithSpecimens);
    } else {
      val updateDonor = donorWithSpecimens.createDonor();
      update(updateDonor);
    }
    return donorId;
  }

}
