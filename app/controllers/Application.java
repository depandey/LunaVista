package controllers;

import models.User;
import models.utils.AppException;
import play.Logger;
import play.data.Form;
import play.data.validation.Constraints;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;


import java.util.List;
import java.util.UUID;

import static play.data.Form.form;

/**
 * Login and Logout.
 * User:
 */
public class Application extends Controller {

    public static Result GO_HOME = redirect(
            routes.Application.index()
    );

    public static Result GO_HOME_ADMIN = redirect(routes.Application.admin());

    public static Result GO_DASHBOARD = redirect(
            routes.Application.index()
    );

    /**
     * Add the content-type json to response
     *
     * @param httpResponse httpResponse
     *
     * @return Result
     */
    public static Result jsonResult(Result httpResponse) {
        response().setContentType("application/json; charset=utf-8");
        return httpResponse;
    }

    /**
     * Display the login page or dashboard if connected
     *
     * @return login page or dashboard
     */
    public Result index() {
        // Check that the email matches a confirmed user before we redirect
        String email = ctx().session().get("email");
        if (email != null) {
            User user = User.findByEmail(email);
            if (user != null && user.validated) {
                return GO_DASHBOARD;
            } else {
                Logger.debug("Clearing invalid session credentials");
                session().clear();
            }
        }

        return ok();
    }

    /**
     * Display the login page or dashboard if connected
     *
     * @return login page or dashboard
     */
    public Result admin() {
        return ok();
    }

    /**
     * Login class used by Login Form.
     */
    public static class Login {

        @Constraints.Required
        public String email;
        @Constraints.Required
        public String password;

        /**
         * Validate the authentication.
         *
         * @return null if validation ok, string with details otherwise
         */
        public Result validate() {

            User user = null;
            try {
                user = User.authenticate(email, password);
            } catch (AppException e) {
                return Application.jsonResult(ok(play.libs.Json.toJson
                        (models.Response.responseBuilder.aresponse().
                                withStatus(Messages.get("application.response.status.failure")).
                                withMessage(Messages.get("application.response.status.failure.message.ERROR_02")).build())));
            }
            if (user == null) {
                return Application.jsonResult(ok(play.libs.Json.toJson
                        (models.Response.responseBuilder.aresponse().
                                withStatus(Messages.get("application.response.status.failure")).
                                withMessage(Messages.get("application.response.status.failure.message.ERROR_01")).build())));
            } else if (!user.validated) {
                return Application.jsonResult(ok(play.libs.Json.toJson
                        (models.Response.responseBuilder.aresponse().
                                withStatus(Messages.get("application.response.status.failure")).
                                withMessage(Messages.get("application.response.status.failure.message.ERROR_04")).build())));
            }
            return null;
        }

    }

    public static class Register {

        @Constraints.Required
        public String email;

        @Constraints.Required
        public String username;

        @Constraints.Required
        public String inputpassword;

        //@Constraints.Required
        public String firstname;

        //@Constraints.Required
        public String lastname;

        public String country;

        /**
         * Validate the authentication.
         *
         * @return null if validation ok, string with details otherwise
         */
        public String validate() {
            if (isBlank(email)) {
                return "Email is required";
            }

            if (isBlank(username)) {
                return "Full name is required";
            }

            if (isBlank(inputpassword)) {
                return "Password is required";
            }
            if(isBlank(country)){
                return "Country is required";
            }

            return null;
        }

        private boolean isBlank(String input) {
            return input == null || input.isEmpty() || input.trim().isEmpty();
        }
    }

    /**
     * Handle login form submission.
     *
     * @return if auth OK or login form if auth KO
     */
    public Result authenticate() {
        Form<Login> loginForm = form(Login.class).bindFromRequest();

        Form<Register> registerForm = form(Register.class);

        if (loginForm.hasErrors()) {
            return badRequest();
        } else {
            User user = null;
            try {
                user = User.authenticate(loginForm.get().email, loginForm.get().password);
            }
            catch (IllegalStateException e){
                Result result = Application.jsonResult(ok(play.libs.Json.toJson
                        (models.Response.responseBuilder.aresponse().
                                withStatus(Messages.get("application.response.status.failure")).
                                withMessage(Messages.get("application.response.status.failure.message.ERROR_01")).build())));
                return result;
            }
            catch (AppException e ) {
                Result result = Application.jsonResult(ok(play.libs.Json.toJson
                        (models.Response.responseBuilder.aresponse().
                                withStatus(Messages.get("application.response.status.failure")).
                                withMessage(Messages.get("application.response.status.failure.message.ERROR_02")).build())));
                return result;
            }
            if(user != null){
                user.auth_key = UUID.randomUUID().toString()+ user.passwordhash;
                user.save();
                session("email", loginForm.get().email);
                String status =  Messages.get("application.response.status.success");
                String message = Messages.get("application.response.status.success.message.SUCCESS_01");
                Result result = (user.admin == true)? GO_DASHBOARD: Application.jsonResult(ok(play.libs.Json.toJson
                        (models.Response.responseBuilder.aresponse().
                                withStatus(status).
                                withData(user).
                                withMessage(message).build())));
                return result;
            }

        }
        Result result = Application.jsonResult(ok(play.libs.Json.toJson
                (models.Response.responseBuilder.aresponse().
                        withStatus(Messages.get("application.response.status.failure")).
                        withMessage(Messages.get("application.response.status.failure.message.ERROR_01")).build())));
        return result;
    }

    /**
     * Logout and clean the session.
     *
     * @return Index page
     */
    public Result logout(String auth_key) {
        try {
            User user = User.findByAuthKey(auth_key);
            user.deleteAuth_key();
            if(user.admin == true){
                session().clear();
                return GO_HOME_ADMIN;
            }
            session().clear();
        }
        catch (Exception e){
            Result result = Application.jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("application.response.status.failure.message.ERROR_03")).build())));
            return result;
        }
        Result result = Application.jsonResult(ok(play.libs.Json.toJson
                (models.Response.responseBuilder.aresponse().
                        withStatus(Messages.get("application.response.status.success")).
                        withMessage(Messages.get("application.response.status.success.message.SUCCESS_02")).build())));
        return result;
    }

    public Result listAll(Long id){

        return Application.jsonResult(ok(play.libs.Json.toJson(User.findById(id))));
    }
}