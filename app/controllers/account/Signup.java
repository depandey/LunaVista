package controllers.account;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.Application;
import models.Response;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import play.libs.ws.*;

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
              return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("application.response.status.failure.message.ERROR_11")).build())));
        }

        Application.Register register = registerForm.get();
        Result resultError = checkBeforeSave(registerForm, register.email,register.username);

        if (resultError != null) {
            return resultError;
        }

        try {
            System.out.println(play.libs.Json.toJson(register));
            User user = new User();
            user.email = register.email;
            user.username = register.username;
            user.passwordhash = Hash.createPassword(register.inputpassword);
            user.confirmationtoken = UUID.randomUUID().toString();
            user.auth_key = UUID.randomUUID().toString()+ user.passwordhash;
            user.dateCreation = new Date();
            String country = register.country;
            if(null == country || country.trim().isEmpty()){
                return jsonResult(ok(play.libs.Json.toJson
                        (models.Response.responseBuilder.aresponse().
                                withStatus(Messages.get("application.response.status.failure")).
                                withMessage(Messages.get("country should not be empty")).build())));
            }
            else{
                String[] locales = Locale.getISOCountries();
                boolean isValidCountry = false;
                for(String countryCode: locales) {
                    Locale obj = new Locale("", countryCode);
                    if(obj.getDisplayCountry().equalsIgnoreCase(country))
                        isValidCountry = true;
                }
                if(isValidCountry) {
                    user.country = country;
                }
                else{
                    return jsonResult(ok(play.libs.Json.toJson
                            (models.Response.responseBuilder.aresponse().
                                    withStatus(Messages.get("application.response.status.failure")).
                                    withMessage(Messages.get("country name does not exist")).build())));
                }
            }
            user.save();
            String urlString = "http://" + Configuration.root().getString("server.hostname");
            urlString += "/confirm/" + user.confirmationtoken;
            URL url = new URL(urlString);
            WSRequest request = ws.url(urlString);
            User.SignUpUserResponseBuilder signUpUserResponseBuilder = new User.SignUpUserResponseBuilder(user);
                if (User.confirm(user)) {
                    return Application.jsonResult(ok(play.libs.Json.toJson
                            (models.Response.responseBuilder.aresponse().
                                    withStatus(Messages.get("application.response.status.success")).
                                    withData(signUpUserResponseBuilder).
                                    withMessage(Messages.get("signup successful")).build())));
                } else {
                    Logger.debug("Signup.confirm cannot confirm user");
                    return badRequest(confirm.render());
                }
        }
        catch (Exception e) {
            Logger.error("Signup.save error", e);
             return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("application.response.status.failure.message.ERROR_02")).build())));
        }
    }

    private String getDate(Date date) {
        SimpleDateFormat formatter;
        formatter = new SimpleDateFormat("dd-MM-yyyy");
        return formatter.format(date);
    }

    public Result update(String auth_key){
        User user = User.findByAuthKey(auth_key);
        if(user == null){
            return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("invalid auth token")).build())));
        }
        JsonNode jsonNode = request().body().asJson();
        if(null == jsonNode){
            return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("please provide data")).build())));
        }
        else{
            String firstName = jsonNode.findPath("firstname").asText();
            if(null == firstName || firstName.trim().isEmpty()){
                return jsonResult(ok(play.libs.Json.toJson
                        (models.Response.responseBuilder.aresponse().
                                withStatus(Messages.get("application.response.status.failure")).
                                withMessage(Messages.get("first name should not be empty")).build())));
            }
            else{
                user.firstname = firstName;
            }
            String lastName = jsonNode.findPath("lastname").asText();
            if(null == lastName || lastName.trim().isEmpty()){
                return jsonResult(ok(play.libs.Json.toJson
                        (models.Response.responseBuilder.aresponse().
                                withStatus(Messages.get("application.response.status.failure")).
                                withMessage(Messages.get("last name should not be empty")).build())));
            }
            else{
                user.lastname = lastName;
            }
            user.update();
            return Application.jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.success")).
                            withData(user).
                            withMessage(Messages.get("update successful")).build())));
        }

    }

    /**
     * Check if the email already exists.
     *
     * @param registerForm User Form submitted
     * @param email
     *@param user_name
     * email address  @return Index if there was a problem, null otherwise
     */
    private Result checkBeforeSave(Form<Application.Register> registerForm, String email, String user_name) {
        // Check unique email
        if (User.findByEmail(email) != null) {
             return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("application.response.status.failure.message.ERROR_05")).build())));
        }
        else if(User.findByUserName(user_name) != null){
            return jsonResult(ok(play.libs.Json.toJson
                    (models.Response.responseBuilder.aresponse().
                            withStatus(Messages.get("application.response.status.failure")).
                            withMessage(Messages.get("user name already taken")).build())));
        }
        else if(!verifyEmailAddress(email)){
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
        urlString += "/confirm/" + user.confirmationtoken;
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
            return badRequest(confirm.render());
        }

        if (user.validated) {
            return badRequest(confirm.render());
        }

        try {
            if (User.confirm(user)) {
                sendMailConfirmation(user);
                flash("success", Messages.get("account.successfully.validated"));
                return ok(confirm.render());
            } else {
                Logger.debug("Signup.confirm cannot confirm user");
                return badRequest(confirm.render());
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
