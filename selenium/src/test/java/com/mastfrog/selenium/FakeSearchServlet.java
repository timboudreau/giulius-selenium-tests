package com.mastfrog.selenium;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author tim
 */
class FakeSearchServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("/favicon.ico".equals(req.getContextPath())) {
            resp.sendError(404);
            return;
        }
        ServletOutputStream out = resp.getOutputStream();
        out.println("<html><head><title>Unit Test</title></head><body><h1>Search</h1>Search for stuff<p/>");
        String q = req.getParameter("searchText");
        out.println("Previous search was: <span id=\"prev\">" + q + "</span><p/>");
        out.println("<form name=\"search\" method=\"get\" action=\"/\">");
        out.println("<input id=\"searchField\" type=\"text\" name=\"searchText\"></input>");
        out.println("<input id=\"searchSubmit\" type=\"submit\"></input>");
        out.println("</form></body></html>");
        out.close();
    }
    
}
