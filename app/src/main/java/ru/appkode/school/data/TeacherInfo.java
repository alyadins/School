package ru.appkode.school.data;

/**
 * Created by lexer on 01.08.14.
 */
public class TeacherInfo {
    public String lastName;
    public String name;
    public String secondName;
    public String subject;

    public TeacherInfo() {
    }

    public TeacherInfo(String lastName, String name, String secondName, String subject) {
        this.lastName = lastName;
        this.name = name;
        this.secondName = secondName;
        this.subject = subject;
    }

    public void setTeacherInfo(TeacherInfo info) {
        this.lastName = info.lastName;
        this.name = info.name;
        this.secondName = info.name;
        this.subject = info.subject;
    }
}
