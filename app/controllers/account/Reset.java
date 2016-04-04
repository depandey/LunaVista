package controllers.account;

import com.fasterxml.jackson.databind.JsonNode;
import models.Token;
import models.User;
import models.utils.AppException;
import models.utils.Hash;
import models.utils.Mail;
import org.apache.commons.mail.EmailException;
import play.Logger;
import play.data.Form;
import play.data.validation.Constraints;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import play.libs.mailer.MailerClient;
import javax.inject.Inject;
import java.net.MalformedURLException;
import java.util.UUID;

import static play.data.Form.form;

/**
 * Token password :
 * - ask for an email address.
 * - send a link pointing them to a reset page.
 * - show the reset page and set them reset it.
 * <p/>
 * <p/>
 * User: yesnault
 * Date: 20/01/12
 */
public class Reset extends Controller {
    @Inject
    MailerClient mailerClient;

    public static class AskForm {
        @Constraints.Required
        public String email;
    }

    public static class ResetForm {
        @Constraints.Required
        public String inputPassword;
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
     * Display the reset password form.
     *
     * @return reset password form
     */
    public Result ask() {
        Form<AskForm> askForm = form(AskForm.class);
        return ok();
    }

    /**
     * Run ask password.
     *
     * @return reset password form if error, runAsk render otherwise
     */
    public Result runAsk() {
        Form<AskForm> askForm = form(AskForm.class).bindFromRequest();

        if (askForm.hasErrors()) {
            flash("error", Messages.get("signup.valid.email"));
            //return badRequest(ask.render(askForm));
           // return jsonResult(ok("{\"status\" : \"failure\", \"message\" : \"invalid email or request\"}"));
            return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("application.response.status.failure.message.ERROR_07")).build())));
        }

        final String email = askForm.get().email;
        Logger.debug("runAsk: email = " + email);
        User user = User.findByEmail(email);
        Logger.debug("runAsk: user = " + user);

        // If we do not have this email address in the list, we should not expose this to the user.
        // This exposes that the user has an account, allowing a user enumeration attack.
        // See http://www.troyhunt.com/2012/05/everything-you-ever-wanted-to-know.html for details.
        // Instead, email the person saying that the reset failed.
        if (user == null) {
            Logger.debug("No user found with email " + email);
            sendFailedPasswordResetAttempt(email);
            //return ok(runAsk.render());
            //return jsonResult(ok("{\"status\" : \"failure\", \"message\" : \"no user found with email\"}"));
            return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("application.response.status.failure.message.ERROR_09")).build())));
        }

        Logger.debug("Sending password reset link to user " + user);

        try {
            Token t = new Token();
            t.sendMailResetPassword(user,mailerClient);
            //return ok(runAsk.render());
            //return jsonResult(ok("{\"status\" : \"success\", \"message\" : \"password reset link send to your email\"}"));
            return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.success")).
                            withMessage(Messages.get("application.response.status.success.message.SUCCESS_04")).build())));
        } catch (MalformedURLException e) {
            Logger.error("Cannot validate URL", e);
            //flash("error", Messages.get("error.technical"));
            return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("application.response.status.failure.message.ERROR_12")).build())));
        }
    }

    /**
     * Sends an email to say that the password reset was to an invalid email.
     *
     * @param email the email address to send to.
     */
    private void sendFailedPasswordResetAttempt(String email) {
        String subject = Messages.get("mail.reset.fail.subject");
        String message = Messages.get("mail.reset.fail.message", email);

        Mail.Envelop envelop = new Mail.Envelop(subject, message, email);
        Mail mailer = new Mail(mailerClient);
        mailer.sendMail(envelop);

    }

    public Result reset(String token) {

        if (token == null) {
            flash("error", Messages.get("error.technical"));
            Form<AskForm> askForm = form(AskForm.class);
            return badRequest();
        }

        Token resetToken = Token.findByTokenAndType(token, Token.TypeToken.password);
        if (resetToken == null) {
            flash("error", Messages.get("error.technical"));
            Form<AskForm> askForm = form(AskForm.class);
            return badRequest();
        }

        if (resetToken.isExpired()) {
            resetToken.delete();
            flash("error", Messages.get("error.expiredresetlink"));
            Form<AskForm> askForm = form(AskForm.class);
            return badRequest();
        }

        Form<ResetForm> resetForm = form(ResetForm.class);
        return ok();
    }

    public Result changePassword(String auth_key){
        try {
            User user = User.findByAuthKey(auth_key);
            if(user != null){
                JsonNode json = request().body().asJson();
                if(json == null) {
                    return jsonResult(ok(play.libs.Json.toJson
                            (models.Response.responseBuilder.aresponse().
                                    withStatus(Messages.get("application.response.status.failure")).
                                    withMessage(Messages.get("application.response.status.failure.message.ERROR_11")).build())));
                } else {
                    String password = json.findPath("inputPassword").textValue();
                    if(password == null) {
                        return jsonResult(ok(play.libs.Json.toJson
                                (models.Response.responseBuilder.aresponse().
                                        withStatus(Messages.get("application.response.status.failure")).
                                        withMessage(Messages.get("application.response.status.failure.message.ERROR_11")).build())));
                    } else {
                        user.passwordhash = Hash.createPassword(password);
                        user.auth_key = UUID.randomUUID().toString()+ user.passwordhash;
                        user.save();
                        // Send email saying that the password has just been changed.
                        sendPasswordChanged(user);
                        return jsonResult(ok(play.libs.Json.toJson
                                (models.Response.responseBuilder.aresponse().
                                        withStatus(Messages.get("application.response.status.success")).
                                        withData(user).
                                        withMessage(Messages.get("application.response.status.success.message.SUCCESS_05")).build())));
                    }
                }
            }
        }
        catch (IllegalStateException e){
            //return ok("{\"status\" : \"failure\", \"message\" : \"authorization key\"}");
            return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("application.response.status.failure.message.ERROR_03")).build())));
        }
        catch (AppException e){
            //return ok("{\"status\" : \"failure\", \"message\" : \"authorization key\"}");
            return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("application.response.status.failure.message.ERROR_13")).build())));
        }
        catch (EmailException e){
            //return ok("{\"status\" : \"failure\", \"message\" : \"authorization key\"}");
            return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("application.response.status.failure.message.ERROR_14")).build())));
        }
        //flash("success", Messages.get("youve.been.logged.out"));
        //return jsonResult(ok(STATUS_SUCCESS+LOGOUT_SUCCESS));
        //return GO_HOME;
        return jsonResult(ok(play.libs.Json.toJson
                (models.Response.responseBuilder.aresponse().
                        withStatus(Messages.get("application.response.status.failure")).
                        withMessage(Messages.get("application.response.status.failure.message.ERROR_13")).build())));
    }

    /**
     * @return reset password form
     */
    public Result runReset(String token) {
        Form<ResetForm> resetForm = form(ResetForm.class).bindFromRequest();

        if (resetForm.hasErrors()) {
            flash("error", Messages.get("signup.valid.password"));
            return badRequest();
        }

        try {
            Token resetToken = Token.findByTokenAndType(token, Token.TypeToken.password);
            if (resetToken == null) {
                flash("error", Messages.get("error.technical"));
                return badRequest();
            }

            if (resetToken.isExpired()) {
                resetToken.delete();
                flash("error", Messages.get("error.expiredresetlink"));
                return badRequest();
            }

            // check email
            User user = User.find.byId(resetToken.userId);
            if (user == null) {
                // display no detail (email unknown for example) to
                // avoir check email by foreigner
                flash("error", Messages.get("error.technical"));
                return badRequest();
            }

            String password = resetForm.get().inputPassword;
            user.changePassword(password);

            // Send email saying that the password has just been changed.
            sendPasswordChanged(user);
            flash("success", Messages.get("resetpassword.success"));
            return ok();
        } catch (AppException e) {
            flash("error", Messages.get("error.technical"));
            return badRequest();
        } catch (EmailException e) {
            flash("error", Messages.get("error.technical"));
            return badRequest();
        }

    }

    /**
     * Send mail with the new password.
     *
     * @param user user created
     * @throws EmailException Exception when sending mail
     */
    private void sendPasswordChanged(User user) throws EmailException {
        String subject = Messages.get("mail.reset.confirm.subject");
        String message = Messages.get("mail.reset.confirm.message");
        Mail.Envelop envelop = new Mail.Envelop(subject, message, user.email);
        Mail mailer = new Mail(mailerClient);
        mailer.sendMail(envelop);
    }
}
