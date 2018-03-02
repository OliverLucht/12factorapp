package net.wasdev.twelvefactorapp;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.net.InetAddress;
import java.net.UnknownHostException;

@WebServlet("/Test")
public class TestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        
        InetAddress ip = null;
        String hostname = "";
        try {
            ip = InetAddress.getLocalHost();
            hostname = ip.getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        
        PrintWriter out = response.getWriter();
        System.out.println("printed - hello world from demo app - to log file");
        System.out.println("Your current IP address : " + ip);
        System.out.println("Your current Hostname : " + hostname);

        try {
            out.println("<!DOCTYPE html>");  // HTML 5
            out.println("<html><head>");
            out.println("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
            out.println("<title>" + "12 Factor Demo Application" + "</title></head>");
            out.println("<body>");
            out.println("<h1>" + "Version 1.0 der 12 Factor Demo Anwendung" + "</h1>");  // Prints "Hello, world!"
            out.println("<h1>" + "" + "</h1>");  
            out.println("<h1>" + "" + "</h1>");  
            out.println("<h4>" + "I.    Codebase:  Eine im Versionsmanagementsystem verwaltete Codebase, viele Deployments" + "</h4>");  // Prints "Hello, world!"
            out.println("<h4>" + "II.   Abhaengigkeiten:  Abhaengigkeiten explizit deklarieren und isolieren" + "</h4>");  // Prints "Hello, world!"
            out.println("<h4>" + "III.  Konfiguration:  Die Konfiguration in Umgebungsvariablen ablegen" + "</h4>");  // Prints "Hello, world!"
            out.println("<h4>" + "IV.   Unterstuetzende Dienste:  Unterstuetzende Dienste als angehaengte Ressourcen behandeln" + "</h4>");  // Prints "Hello, world!"
            out.println("<h4>" + "V.    Build, release, run:  Build- und Run-Phase strikt trennen" + "</h4>");  // Prints "Hello, world!"
            out.println("<h4>" + "VI.   Prozesse:  Die App als einen oder mehrere Prozesse ausfuehren" + "</h4>");  // Prints "Hello, world!"
            out.println("<h4>" + "VII.  Bindung an Ports:  Dienste durch das Binden von Ports exportieren" + "</h4>");  // Prints "Hello, world!"
            out.println("<h4>" + "VIII. Nebenlaeufigkeit:  Mit dem Prozess-Modell skalieren" + "</h4>");  // Prints "Hello, world!"
            out.println("<h4>" + "IX.   Einweggebrauch:  Robuster mit schnellem Start und problemlosen Stopp" + "</h4>");  // Prints "Hello, world!"
            out.println("<h4>" + "X.    Dev-Prod-Vergleichbarkeit:  Entwicklung, Staging und Produktion so aehnlich wie moeglich halten" + "</h4>");  // Prints "Hello, world!"
            out.println("<h4>" + "XI.   Logs:  Logs als Strom von Ereignissen behandeln" + "</h4>");  // Prints "Hello, world!"
            out.println("<h4>" + "XII.  Admin-Prozesse:  Admin/Management-Aufgaben als einmalige Vorgaenge behandeln" + "</h4>");  // Prints "Hello, world!"
            out.println("<h1>" + "" + "</h1>");  
            out.println("<h1>" + "" + "</h1>");  
            out.println("<h1>" + "_________________________________________________________________________________________" + "</h1>"); 
            out.println("<h8>" + "Current IP: " + ip + "</h8>"); 
        } finally {
            out.close();  // Always close the output writer
        }
    }
}
