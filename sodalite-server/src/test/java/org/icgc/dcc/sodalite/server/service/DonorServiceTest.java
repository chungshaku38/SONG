package org.icgc.dcc.sodalite.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.test.annotation.FlywayTest;
import org.flywaydb.test.junit.FlywayTestExecutionListener;
import org.icgc.dcc.sodalite.server.model.entity.Donor;
import org.icgc.dcc.sodalite.server.model.entity.Specimen;
import org.icgc.dcc.sodalite.server.model.enums.DonorGender;
import org.icgc.dcc.sodalite.server.utils.JsonUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import lombok.val;

@SpringBootTest
@RunWith(SpringRunner.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@FlywayTest
@ActiveProfiles("dev")
public class DonorServiceTest {

  @Autowired
  DonorService service;
  @Autowired
  SpecimenService specimenService;

  @Test
  public void testReadDonor() {
    // check for data that we know exists in the H2 database already
    val d = service.read("DO1");
    assertThat(d != null);
    assertThat(d.getDonorId()).isEqualTo("DO1");
    assertThat(d.getDonorGender()).isEqualTo(DonorGender.MALE.value());
    assertThat(d.getDonorSubmitterId()).isEqualTo("Subject-X23Alpha7");
    assertThat(d.getSpecimens().size()).isEqualTo(2);

    // Just check that each specimen object that we get is the same as the one we get from the
    // specimen service. Let the specimen service tests verify that the contents are right.
    d.getSpecimens().forEach(specimen -> assertThat(specimen.equals(getMatchingSpecimen(specimen))));

  }

  Specimen getMatchingSpecimen(Specimen specimen) {
    return specimenService.getById(specimen.getSpecimenId());
  }

  @Test
  public void testCreateAndDeleteDonor() {
    val json = JsonUtils.mapper().createObjectNode();
    val studyId = "XYZ234";
    json.put("donorId", "");
    json.put("donorSubmitterId", "Subject X21-Alpha");
    json.put("studyId", studyId);
    json.put("donorGender", "unspecified");
    json.put("notes", "There are no notes for this donor");
    json.put("ageCategory", 7);

    val d = JsonUtils.mapper().convertValue(json, Donor.class);

    assertThat(d.getDonorId()).isEqualTo("");

    val status = service.create(d);
    val id = d.getDonorId();

    assertThat(id).startsWith("DO");
    assertThat(status).isEqualTo("ok:" + id);

    Donor check = service.read(id);
    assertThat(d).isEqualToComparingFieldByField(check);

    service.delete("XYZ234", id);
    Donor check2 = service.read(id);
    assertThat(check2).isNull();
  }

  @Test
  public void testUpdateDonor() {
    val studyId = "ABC123";

    val d = new Donor("", "Triangle-Arrow-S", studyId, DonorGender.MALE);
    service.create(d);

    val id = d.getDonorId();

    val d2 = new Donor(id, "X21-Beta-17", studyId, DonorGender.FEMALE);

    service.update(d2);

    val d3 = service.read(id);
    assertThat(d3).isEqualToComparingFieldByField(d2);
  }

}
