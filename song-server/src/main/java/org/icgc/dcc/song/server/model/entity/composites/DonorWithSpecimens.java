package org.icgc.dcc.song.server.model.entity.composites;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.icgc.dcc.song.server.model.entity.Donor;

import java.util.ArrayList;
import java.util.List;
@EqualsAndHashCode
@Value
public class DonorWithSpecimens extends Donor {
    private List<SpecimenWithSamples> specimens = new ArrayList<>();

    @JsonIgnore
    public void setDonor(Donor d) {
        this.setDonorId(d.getDonorId());
        this.setDonorSubmitterId(d.getDonorSubmitterId());
        this.setStudyId(d.getStudyId());
        this.setDonorGender(d.getDonorGender());
        this.setDonorSubmitterId(d.getDonorSubmitterId());
        this.addInfo(d.getInfo());
    }

    @JsonIgnore
    public Donor getDonor() {
        return Donor.create(getDonorId(),getDonorSubmitterId(),getStudyId(), getDonorGender(), getInfo());
    }

    public void addSpecimen(SpecimenWithSamples s) {
        specimens.add(s);
    }

    public void setSpecimens(List<SpecimenWithSamples> s) {
        specimens.clear();
        specimens.addAll(s);
    }

}