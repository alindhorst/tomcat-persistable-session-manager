package de.mobile.sessiontest;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

/**
 *
 * @author alindhorst
 */
@WebServlet(name = "HelloServlet", urlPatterns = {"/Hello"})
public class HelloServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            int responseCode = SC_OK;
            HttpSession session = request.getSession(false);
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("<!DOCTYPE html>").append("<html>").append("<head>").append("</head>").append(
                    "<body>");
            if (session != null) {
                responseBuilder.append("<h1>Hello, I found a session in your request: ").append(session.getId())
                        .append("</h1><div>This is your ").append(getAndIncrementCounter(session)).append(
                        " visit.</div>");
            } else {
                responseCode = SC_UNAUTHORIZED;
                session = request.getSession(true);
                responseBuilder.append("<h1>Hello, I created a new session:").append(session.getId()).append("</h1>");
                getAndIncrementCounter(session);
            }
            responseBuilder.append("<a href=\"").append(response.encodeURL(request.getRequestURL().toString()))
                    .append("\">Reload</a>").append("</body>").append("</html>");

            response.setStatus(responseCode);
            out.print(responseBuilder.toString());
        }
    }

    private int getAndIncrementCounter(HttpSession session) {
        Integer value = (Integer) session.getAttribute("counter");
        if (value == null) {
            session.setAttribute("counter", 0);
        }
        int counter = (int) session.getAttribute("counter");
        counter++;
        session.setAttribute("counter", counter);
        return counter;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
