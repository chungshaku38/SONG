/*
 * Copyright (c) 2017 The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.icgc.dcc.song.server.service;

import static org.icgc.dcc.song.server.model.enums.IdPrefix.Donor;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;
import org.icgc.dcc.song.server.model.entity.Donor;
import org.icgc.dcc.song.server.model.entity.composites.DonorWithSpecimens;
import org.icgc.dcc.song.server.model.enums.IdPrefix;
import org.icgc.dcc.song.server.repository.DonorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
@Service
public class DonorService {

  @Autowired
  private final DonorRepository donorRepository;
  @Autowired
  private final IdService idService;
  @Autowired
  private final SpecimenService specimenService;

  public String create(@NonNull DonorWithSpecimens d) {
    val id = idService.generate(Donor);
    d.setDonorId(id);

    val status = donorRepository.create(d.getDonor());
    if (status != 1) {
      return "error: Can't create" + d.toString();
    }
    d.getSpecimens().forEach(s -> specimenService.create(id, s));

    return id;
  }

  public Donor read(@NonNull String id) {
    return donorRepository.read(id);
  }

  public DonorWithSpecimens readWithSpecimens(@NonNull String id) {
    val donor = new DonorWithSpecimens();
    donor.setDonor(read(id));

    donor.setSpecimens(specimenService.readByParentId(id));
    return donor;
  }

  public List<DonorWithSpecimens> readByParentId(@NonNull String parentId) {
    val donors = new ArrayList<DonorWithSpecimens>();
    val ids = donorRepository.findByParentId(parentId);
    ids.forEach(id -> donors.add(readWithSpecimens(id)));

    return donors;
  }

  public String update(@NonNull Donor donor) {
    if (donorRepository.update(donor) == 1) {
      return "Updated";
    }
    return "Failed";
  }

  public String delete(@NonNull String studyId, @NonNull String id) {
    specimenService.deleteByParentId(id);
    donorRepository.delete(studyId, id);
    return "OK";
  }

  public String deleteByParentId(@NonNull String studyId) {
    donorRepository.findByParentId(studyId).forEach(id -> delete(studyId, id));
    return "OK";
  }

  public String save(@NonNull String studyId, @NonNull Donor donor) {
    donor.setStudyId(studyId);

    String donorId = donorRepository.findByBusinessKey(studyId, donor.getDonorSubmitterId());
    if (donorId == null) {
      donorId = idService.generate(IdPrefix.Donor);
      donor.setDonorId(donorId);
      System.err.printf("Creating new donor with id=%s,gender='%s'\n", donorId, donor.getDonorGender());
      donorRepository.create(donor);
    } else {
      donorRepository.update(donor);
    }
    return donorId;
  }

}