package com.tracker.expense;

import com.tracker.expense.servlet.ExpenseServlet;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ExpenseTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenseTrackerApplication.class, args);
    }


    @Bean
    public ServletRegistrationBean<ExpenseServlet> expenseServletRegistration() {
        ServletRegistrationBean<ExpenseServlet> bean = new ServletRegistrationBean<>(new ExpenseServlet(), "/legacy-servlet");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    public CommandLineRunner printClickableLinks() {
        return args -> {
            System.out.println("\n------------------------------------------------------------------");
            System.out.println(" Expense Tracker Application Started Successfully (₹)! ");
            System.out.println(" Web Application UI : http://localhost:8080/");
            System.out.println(" REST API Endpoint : http://localhost:8080/api/expenses");
            System.out.println(" Legacy Servlet    : http://localhost:8080/legacy-servlet");
            System.out.println("------------------------------------------------------------------\n");
        };
    }
}