package org.icgc.dcc.sodalite.server.repository;

import java.util.List;

import org.icgc.dcc.sodalite.server.model.entity.File;
import org.icgc.dcc.sodalite.server.repository.mapper.FileMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(FileMapper.class)
public interface FileRepository {

  @SqlUpdate("INSERT INTO File (id, name, sample_id, size, type, md5, metadata_doc) VALUES (:id, :name, :sample_id, :size, :file_type, :file_md5, :metadata_doc)")
  int save(@Bind("id") String id, @Bind("sample_id") String sample_id, @Bind("name") String name,
      @Bind("size") Long size, @Bind("file_type") String fileType, @Bind("file_md5") String fileMd5,
      @Bind("metadata_doc") String metadataDoc);

  @SqlUpdate("UPDATE File SET name=:name, size=:size, type=:file_type, md5=:file_md5, metadata_doc=:metadata_doc where id=:id")
  int set(@Bind("id") String id, @Bind("name") String name, @Bind("size") Long size, @Bind("file_type") String fileType,
      @Bind("file_md5") String fileMd5, @Bind("metadata_doc") String metadataDoc);

  @SqlUpdate("DELETE From File where id=:id")
  int delete(@Bind("id") String id);

  @SqlUpdate("DELETE From File where sample_id=:sample_id")
  int deleteBySampleId(@Bind("sample_id") String sample_id);

  @SqlQuery("SELECT id, name, sample_id, size, type, md5, metadata_doc FROM File WHERE id=:id")
  File getById(@Bind("id") String id);

  @SqlQuery("SELECT id, name, sample_id, size, type, md5, metadata_doc FROM File WHERE sample_id=:sample_id")
  List<File> findByParentId(@Bind("sample_id") String sample_id);

  @SqlQuery("SELECT f.id from File f, Sample s, Specimen sp, Donor d "
      + "WHERE f.name=:fileName "
      + "AND f.sample_id = s.id "
      + "AND s.specimen_id = sp.id "
      + "AND sp.donor_id = d.id "
      + "AND d.study_id = :studyId")
  String getIdByBusinessKey(@Bind("studyId") String studyId, @Bind("fileName") String fileName);
}
