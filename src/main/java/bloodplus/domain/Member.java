package bloodplus.domain;

/** 회원 MEMBER (아이디, 비밀번호, 이름, 연락처, 주소) */
public class Member {
    private String id;
    private String password;
    private String name;
    private String phone;
    private String address;

    public Member() {}
    public Member(String id, String password, String name, String phone, String address) {
        this.id = id; this.password = password; this.name = name; this.phone = phone; this.address = address;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    /** 주소 맨 앞 시/도 (예: 서울특별시) */
    public String getCity() { return address == null ? "" : address.split(" ")[0]; }
}
