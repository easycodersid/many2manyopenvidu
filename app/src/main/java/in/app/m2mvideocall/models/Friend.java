package in.app.m2mvideocall.models;

public class Friend {

    String name, email, user_id;

    public Friend(String name, String email, String user_id){
        this.name = name;
        this.email = email;
        this.user_id = user_id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getUser_id() {
        return user_id;
    }
}
