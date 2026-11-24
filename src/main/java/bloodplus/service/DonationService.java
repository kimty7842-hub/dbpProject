package bloodplus.service;

import bloodplus.domain.Donation;
import bloodplus.repository.BloodRepository;
import bloodplus.repository.DbException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** 헌혈 이력: 조회, 등록, 수정, 삭제 */
@Service
public class DonationService {

    private final BloodRepository repository;

    public DonationService(BloodRepository repository) { this.repository = repository; }

    public List<Donation> list(String memberId) { return repository.findDonations(memberId); }

    public Donation find(int no, String memberId) {
        return repository.findDonation(no, memberId).orElseThrow(() -> new DbException("헌혈 기록을 찾을 수 없습니다."));
    }

    public void add(String memberId, String date, String type, String region) {
        repository.saveDonation(new Donation(0, memberId, parse(date), type, region));
    }

    public void update(int no, String memberId, String date, String type, String region) {
        repository.updateDonation(new Donation(no, memberId, parse(date), type, region));
    }

    public void delete(int no, String memberId) { repository.deleteDonation(no, memberId); }

    private static LocalDate parse(String text) {
        LocalDate d;
        try { d = LocalDate.parse(text == null ? "" : text.trim()); }
        catch (DateTimeParseException e) { throw new DbException("헌혈한 날을 선택해 주시기 바랍니다."); }
        if (d.isAfter(LocalDate.now())) throw new DbException("오늘 이후 날짜는 등록할 수 없습니다.");
        return d;
    }
}
