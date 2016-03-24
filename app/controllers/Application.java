package controllers;

import models.Urls;
import models.User;
import models.utils.AppException;
import play.Logger;
import play.api.libs.json.Json;
import play.data.Form;
import play.data.validation.Constraints;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.admin;
import views.html.index;

import java.net.HttpURLConnection;
import java.net.URL;
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
            routes.Dashboard.index()
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
                //return jsonResult(ok(STATUS_SUCCESS + play.libs.Json.toJson(user) + LOGIN_SUCCESS ));
                /*return jsonResult(ok(play.libs.Json.toJson
                                    (models.Response.responseBuilder.aresponse().
                                            withStatus(Messages.get("application.response.status.success")).
                                            withData(user).
                                            withMessage(Messages.get("application.response.status.success.message.SUCCESS_01")).build())));*/
            } else {
                Logger.debug("Clearing invalid session credentials");
                session().clear();
            }
        }

        return ok(index.render(form(Register.class), form(Login.class)));
    }

    /**
     * Display the login page or dashboard if connected
     *
     * @return login page or dashboard
     */
    public Result admin() {
        return ok(admin.render(form(Login.class)));
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
                //return Messages.get("error.technical");
                //return ok("{\"status\" : \"failure\", \"message\" : \"technical error. please check config\"}");
                return Application.jsonResult(ok(play.libs.Json.toJson
                        (models.Response.responseBuilder.aresponse().
                                withStatus(Messages.get("application.response.status.failure")).
                                withMessage(Messages.get("application.response.status.failure.message.ERROR_02")).build())));
            }
            if (user == null) {
               // return Messages.get("invalid.user.or.password");
                //return ok("{\"status\" : \"failure\", \"message\" : \"invalid user name or password\"}");
                return Application.jsonResult(ok(play.libs.Json.toJson
                        (models.Response.responseBuilder.aresponse().
                                withStatus(Messages.get("application.response.status.failure")).
                                withMessage(Messages.get("application.response.status.failure.message.ERROR_01")).build())));
            } else if (!user.validated) {
               // return Messages.get("account.not.validated.check.mail");
               // return ok("{\"status\" : \"failure\", \"message\" : \"account not validated check email\"}");
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
        public String userName;

        @Constraints.Required
        public String inputPassword;

        //@Constraints.Required
        public String firstName;

        //@Constraints.Required
        public String lastName;

        /**
         * Validate the authentication.
         *
         * @return null if validation ok, string with details otherwise
         */
        public String validate() {
            if (isBlank(email)) {
                return "Email is required";
            }

            if (isBlank(userName)) {
                return "Full name is required";
            }

            if (isBlank(inputPassword)) {
                return "Password is required";
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
     * @return Dashboard if auth OK or login form if auth KO
     */
    public Result authenticate() {
        Form<Login> loginForm = form(Login.class).bindFromRequest();

        Form<Register> registerForm = form(Register.class);

        if (loginForm.hasErrors()) {
            return badRequest(index.render(registerForm, loginForm));
        } else {
            User user = null;
            try {
                user = User.authenticate(loginForm.get().email, loginForm.get().password);
            }
            catch (IllegalStateException e){
               // return ok("{\"status\" : \"failure\", \"message\" : \"invalid email or password\"}");
                return Application.jsonResult(ok(play.libs.Json.toJson
                        (models.Response.responseBuilder.aresponse().
                                withStatus(Messages.get("application.response.status.failure")).
                                withMessage(Messages.get("application.response.status.failure.message.ERROR_01")).build())));
            }
            catch (AppException e ) {
                //return Messages.get("error.technical");
               // return ok("{\"status\" : \"failure\", \"message\" : \"technical error. please check config\"}");
                return Application.jsonResult(ok(play.libs.Json.toJson
                        (models.Response.responseBuilder.aresponse().
                                withStatus(Messages.get("application.response.status.failure")).
                                withMessage(Messages.get("application.response.status.failure.message.ERROR_02")).build())));
            }
            /*session("email", loginForm.get().email);
            User user = User.findByEmail(loginForm.get().email);
            //return GO_DASHBOARD;*/
            if(user != null){
                //return jsonResult(ok(STATUS_SUCCESS + DATA+ play.libs.Json.toJson(user) + LOGIN_SUCCESS ));
                user.auth_key = UUID.randomUUID().toString()+ user.passwordHash;
                user.save();
                session("email", loginForm.get().email);
                return (user.admin == true)? GO_DASHBOARD: Application.jsonResult(ok(play.libs.Json.toJson
                        (models.Response.responseBuilder.aresponse().
                                withStatus(Messages.get("application.response.status.success")).
                                withData(user).
                                withMessage(Messages.get("application.response.status.success.message.SUCCESS_01")).build())));
            }

        }
        //return ok("{\"status\" : \"failure\", \"message\" : \"Invalid email or passoword\"}");
        return Application.jsonResult(ok(play.libs.Json.toJson
                (models.Response.responseBuilder.aresponse().
                        withStatus(Messages.get("application.response.status.failure")).
                        withMessage(Messages.get("application.response.status.failure.message.ERROR_01")).build())));
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
            //return ok("{\"status\" : \"failure\", \"message\" : \"authorization key\"}");
            return Application.jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("application.response.status.failure.message.ERROR_03")).build())));
        }
        //flash("success", Messages.get("youve.been.logged.out"));
        //return jsonResult(ok(STATUS_SUCCESS+LOGOUT_SUCCESS));
        //return GO_HOME;
        return Application.jsonResult(ok(play.libs.Json.toJson
                (models.Response.responseBuilder.aresponse().
                        withStatus(Messages.get("application.response.status.success")).
                        withMessage(Messages.get("application.response.status.success.message.SUCCESS_02")).build())));
    }

    public Result getUrls(String auth_key){
        User user = User.findByAuthKey(auth_key);
        if(user != null){
            List<Urls> urls = Urls.getAll();
            return Application.jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.success")).
                            withData(urls).
                            withMessage(Messages.get("application.response.status.success.message.SUCCESS_02")).build())));
        }
        return ok();
    }
}