package bloodplus.service;

import bloodplus.domain.Member;
import bloodplus.repository.BloodRepository;
import bloodplus.repository.DbException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** 회원: 로그인, 가입, 정보 조회와 수정, 탈퇴 */
@Service
public class MemberService {

    private final BloodRepository repository;

    public MemberService(BloodRepository repository) { this.repository = repository; }

    public Optional<Member> login(String id, String password) {
        return repository.findMemberByIdAndPassword(id, password);
    }

    public Member find(String id) {
        return repository.findMemberById(id).orElseThrow(() -> new DbException("회원 정보를 찾을 수 없습니다."));
    }

    /** 회원가입. 입력값 검사 후 INSERT. 아이디 중복은 DB 의 기본키가 막는다. */
    public void join(Member m, String passwordConfirm) {
        if (m.getName() == null || m.getName().trim().length() < 2) throw new DbException("이름을 2글자 이상 입력해 주시기 바랍니다.");
        if (m.getId() == null || !m.getId().matches("[A-Za-z0-9]{4,12}")) throw new DbException("아이디는 영문, 숫자 4~12자로 입력해 주시기 바랍니다.");
        if (m.getPassword() == null || m.getPassword().length() < 4) throw new DbException("비밀번호는 4자 이상이어야 합니다.");
        if (!m.getPassword().equals(passwordConfirm)) throw new DbException("비밀번호가 서로 다릅니다.");
        checkContact(m);
        repository.saveMember(m);
    }

    /** 정보 수정. 새 비밀번호가 비어 있으면 비밀번호는 바꾸지 않는다. */
    public void update(Member m, String newPassword, String passwordConfirm) {
        if (m.getName() == null || m.getName().trim().length() < 2) throw new DbException("이름을 2글자 이상 입력해 주시기 바랍니다.");
        checkContact(m);
        if (newPassword != null && !newPassword.isEmpty()) {
            if (newPassword.length() < 4) throw new DbException("비밀번호는 4자 이상이어야 합니다.");
            if (!newPassword.equals(passwordConfirm)) throw new DbException("비밀번호가 서로 다릅니다.");
            m.setPassword(newPassword);
        } else {
            m.setPassword(null);
        }
        repository.updateMember(m);
    }

    public void withdraw(String id) { repository.deleteMember(id); }

    private static void checkContact(Member m) {
        if (m.getPhone() == null || !m.getPhone().trim().matches("01[0-9]-?\\d{3,4}-?\\d{4}")) throw new DbException("연락처를 010-0000-0000 형식으로 입력해 주시기 바랍니다.");
        if (m.getAddress() == null || m.getAddress().trim().split(" ").length < 2) throw new DbException("상세 주소를 입력해 주시기 바랍니다.");
    }
}
