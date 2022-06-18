package in.app.m2mvideocall.models;

public class Member {

    public String name, id;
    public boolean approved;

    public Member(String name, String id, boolean approved){
        this.name = name;
        this.id = id;
        this.approved = approved;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public boolean isApproved() {
        return approved;
    }
}
