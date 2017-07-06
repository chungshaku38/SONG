package org.icgc.dcc.song.server.importer.processor;

import lombok.NonNull;
import lombok.val;
import org.icgc.dcc.song.server.importer.convert.Converters;
import org.icgc.dcc.song.server.importer.model.PortalDonorMetadata;
import org.icgc.dcc.song.server.model.entity.Study;
import org.icgc.dcc.song.server.repository.DonorRepository;
import org.icgc.dcc.song.server.repository.SampleRepository;
import org.icgc.dcc.song.server.repository.SpecimenRepository;
import org.icgc.dcc.song.server.repository.StudyRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.icgc.dcc.song.server.importer.convert.Converters.convertToDonor;
import static org.icgc.dcc.song.server.importer.convert.Converters.convertToSpecimens;
import static org.icgc.dcc.song.server.importer.convert.Converters.convertToStudy;

public class DonorProcessor implements Runnable {

  @NonNull private final List<PortalDonorMetadata> donors;
  @Autowired DonorRepository donorRepository;
  @Autowired SpecimenRepository specimenRepository;
  @Autowired SampleRepository sampleRepository;
  @Autowired private StudyRepository studyRepository;

  /**
   * State
   */
  private Set<Study> studySet = newHashSet();

  private DonorProcessor(List<PortalDonorMetadata> donors) {
    this.donors = donors;
  }

  @Override
  public void run() {
    for (val donorMetadata : donors){
      updateStudy(donorMetadata);
      val donor = convertToDonor(donorMetadata);
      donorRepository.create(donor);
      convertToSpecimens(donorMetadata).forEach(specimenRepository::create);
      donorMetadata.getSpecimens().stream()
          .map(Converters::convertToSamples)
          .flatMap(Collection::stream)
          .forEach(sampleRepository::create);
    }
  }

  private void updateStudy(PortalDonorMetadata donorMetadata){
    val study = convertToStudy(donorMetadata);
    if (!studySet.contains(study)){
      studySet.add(study);
//      studyRepository.create(study.getStudyId(),study.getName(),study.getOrganization(),study.getDescription());
      studyRepository.create(study);
    }
  }

  public static DonorProcessor createDonorProcessor(List<PortalDonorMetadata> donors) {
    return new DonorProcessor(donors);
  }

}
