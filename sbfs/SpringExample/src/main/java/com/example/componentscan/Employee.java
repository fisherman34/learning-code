package com.example.componentscan;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// EmployeeクラスをSpringのBeanとして登録する。
// SpringのComponent Scanによってこのクラスが検出されると、
// Spring ContainerがEmployeeのインスタンスを生成・管理する。
//
// "employee" はSpring Containerに登録されるBean名。
// そのため、Bean名を指定して取得する場合は、
// context.getBean("employee") のように指定できる。
@Component("employee")
public class Employee {
    private int employeeId;

    // @Value は、SpringがBeanを生成するときにフィールドへ値を注入するためのアノテーション。
    // 固定文字列 "Hello" をfirstNameに注入する。
    //
    // SpringがEmployee Beanを生成すると、
    //
    //     firstName = "Hello"
    //
    // となる。
    //
    // 外部設定ファイルの値を参照しているわけではなく、
    // "Hello" という文字列そのものを指定している。
    @Value("Hello")
    private String firstName;

    // Springのプロパティ（Environment / properties）から
    // "java.home" というプロパティの値を取得して注入する。
    //
    // ${...} は「プロパティプレースホルダー」を表す。
    //
    // この例では、実行環境のJava Homeの値が
    // lastNameに設定される。
    //
    // 例えば環境によって、
    //
    //     /usr/lib/jvm/java-17
    //
    // のような値になる。
    @Value("${java.home}")
    private String lastName;

    // SpEL（Spring Expression Language）を使用して
    // 式 "#{4*4}" を評価する。
    //
    // 4 * 4 = 16 と計算され、
    //
    //     salary = 16
    //
    // が設定される。
    //
    // "#{...}" はSpELの式を表す。
    @Value("#{4*4}")
    private double salary;

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "employeeId=" + employeeId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", salary=" + salary +
                '}';
    }
}
