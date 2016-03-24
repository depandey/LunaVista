package controllers.account;

import controllers.Application;
import models.User;
import models.utils.AppException;
import models.utils.Hash;
import models.utils.Mail;
import org.apache.commons.mail.EmailException;
import play.Configuration;
import play.Logger;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import play.libs.mailer.MailerClient;

import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import views.html.account.signup.confirm;
import views.html.account.signup.create;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import javax.inject.Inject;

import play.mvc.*;
import play.libs.ws.*;
import play.libs.F.Function;
import play.libs.F.Promise;
import static play.data.Form.form;

/**
 * Signup to PlayStartApp : save and send confirm mail.
 * <p/>
 * User: yesnault
 * Date: 31/01/12
 */
public class Signup extends Controller {
    @Inject
    MailerClient mailerClient;

    @Inject WSClient ws;

    /**
     * Display the create form.
     *
     * @return create form
     */
    public Result create() {
        return ok(create.render(form(Application.Register.class)));
    }

    /**
     * Display the create form only (for the index page).
     *
     * @return create form
     */
    public Result createFormOnly() {
        return ok(create.render(form(Application.Register.class)));
    }

    /**
     * Add the content-type json to response
     *
     * @param httpResponse httpResponse
     *
     * @return Result
     */
    public Result jsonResult(Result httpResponse) {
        response().setContentType("application/json; charset=utf-8");
        return httpResponse;
    }

    /**
     * Save the new user.
     *
     * @return Successfull page or created form if bad
     */
    public Result save() {
        Form<Application.Register> registerForm = form(Application.Register.class).bindFromRequest();

        if (registerForm.hasErrors()) {
            //return badRequest(create.render(registerForm));
            //return jsonResult(ok("{\"status\" : \"failure\", \"message\" : \"invalid request\"}"));
            return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("application.response.status.failure.message.ERROR_11")).build())));
        }

        Application.Register register = registerForm.get();
        Result resultError = checkBeforeSave(registerForm, register.email);

        if (resultError != null) {
            return resultError;
        }

        try {
            User user = new User();
            user.email = register.email;
            user.userName = register.userName;
            user.firstName = register.firstName;
            user.lastName = register.lastName;
            user.passwordHash = Hash.createPassword(register.inputPassword);
            user.confirmationToken = UUID.randomUUID().toString();
            user.auth_key = UUID.randomUUID().toString()+ user.passwordHash;

            user.save();
            //sendMailAskForConfirmation(user);

            String urlString = "http://" + Configuration.root().getString("server.hostname");
            urlString += "/confirm/" + user.confirmationToken;
            URL url = new URL(urlString);
            WSRequest request = ws.url(urlString);
                if (User.confirm(user)) {
                   // sendMailConfirmation(user);
                    //flash("success", Messages.get("account.successfully.validated"));
                    //return ok(confirm.render());
                    return jsonResult(ok("{\"status\" : \"success\", \"message\" : \"account successfully created\"}"));
                } else {
                    Logger.debug("Signup.confirm cannot confirm user");
                    //flash("error", Messages.get("error.confirm"));
                    return badRequest(confirm.render());
                    //return jsonResult(ok("{\"status\" : \"failure\", \"message\" : \"confirmation failed\"}"));
                }
        }
        catch (Exception e) {
            Logger.error("Signup.save error", e);
            //flash("error", Messages.get("error.technical"));
           //return jsonResult(ok("{\"status\" : \"failure\", \"message\" : \"technical error. check config\"}"));
            return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("application.response.status.failure.message.ERROR_02")).build())));
        }
        //return badRequest(create.render(registerForm));
    }

    /**
     * Check if the email already exists.
     *
     * @param registerForm User Form submitted
     * @param email        email address
     * @return Index if there was a problem, null otherwise
     */
    private Result checkBeforeSave(Form<Application.Register> registerForm, String email) {
        // Check unique email
        if (User.findByEmail(email) != null) {
            //flash("error", Messages.get("error.email.already.exist"));
           //return jsonResult(ok("{\"status\" : \"failure\", \"message\" : \"email address already taken\"}"));
            return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("application.response.status.failure.message.ERROR_05")).build())));
           // return badRequest(create.render(registerForm));
        }
        else if(!verifyEmailAddress(email)){
           // return jsonResult(ok("{\"status\" : \"failure\", \"message\" : \"cannot verify email address\"}"));
            return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("application.response.status.failure.message.ERROR_06")).build())));
        }

        return null;
    }

    private boolean verifyEmailAddress(String email) {
        try {
            InternetAddress internetAddress = new InternetAddress(email);
            internetAddress.validate();
            return true;
        } catch (AddressException e) {
            return false;
        }
    }

    /**
     * Send the welcome Email with the link to confirm.
     *
     * @param user user created
     * @throws EmailException Exception when sending mail
     */
    private void sendMailAskForConfirmation(User user) throws EmailException, MalformedURLException {
        String subject = Messages.get("mail.confirm.subject");

        String urlString = "http://" + Configuration.root().getString("server.hostname");
        urlString += "/confirm/" + user.confirmationToken;
        URL url = new URL(urlString); // validate the URL, will throw an exception if bad.
        String message = Messages.get("mail.confirm.message", url.toString());

        Mail.Envelop envelop = new Mail.Envelop(subject, message, user.email);
        Mail mailer = new Mail(mailerClient);
        mailer.sendMail(envelop);
    }

    /**
     * Valid an account with the url in the confirm mail.
     *
     * @param token a token attached to the user we're confirming.
     * @return Confirmation page
     */
    public Result confirm(String token) {
        User user = User.findByConfirmationToken(token);
        if (user == null) {
            //flash("error", Messages.get("error.unknown.email"));
            return badRequest(confirm.render());
           // return jsonResult(ok("{\"status\" : \"failure\", \"message\" : \"unknow email\"}"));
        }

        if (user.validated) {
           // flash("error", Messages.get("error.account.already.validated"));
            return badRequest(confirm.render());
            //return jsonResult(ok("{\"status\" : \"failure\", \"message\" : \"account already validated\"}"));
        }

        try {
            if (User.confirm(user)) {
                sendMailConfirmation(user);
                flash("success", Messages.get("account.successfully.validated"));
                return ok(confirm.render());
                //return jsonResult(ok("{\"status\" : \"success\", \"message\" : \"account successfully validated\"}"));
            } else {
                Logger.debug("Signup.confirm cannot confirm user");
                //flash("error", Messages.get("error.confirm"));
                return badRequest(confirm.render());
                //return jsonResult(ok("{\"status\" : \"failure\", \"message\" : \"confirmation failed\"}"));
            }
        } catch (AppException e) {
            Logger.error("Cannot signup", e);
            flash("error", Messages.get("error.technical"));
        } catch (EmailException e) {
            Logger.debug("Cannot send email", e);
            flash("error", Messages.get("error.sending.confirm.email"));
        }
        return badRequest(confirm.render());
    }

    /**
     * Send the confirm mail.
     *
     * @param user user created
     * @throws EmailException Exception when sending mail
     */
    private void sendMailConfirmation(User user) throws EmailException {
        String subject = Messages.get("mail.welcome.subject");
        String message = Messages.get("mail.welcome.message");
        Mail.Envelop envelop = new Mail.Envelop(subject, message, user.email);
        Mail mailer = new Mail(mailerClient);
        mailer.sendMail(envelop);
    }
}
