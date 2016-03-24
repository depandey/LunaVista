package controllers;

import models.Urls;
import models.User;
import play.data.Form;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.account.settings.emailValidate;
import views.html.dashboard.index;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static play.data.Form.form;

/**
 * User: yesnault
 * Date: 22/01/12
 */
@Security.Authenticated(Secured.class)
public class Dashboard extends Controller {

    private static final String REGEX = "\\s*(\\s|\\r|\\n|,)\\s*";

    public static Result GO_DASHBOARD = redirect(
            routes.Dashboard.index()
    );

    public Result index() {
        Form<Urls> urlForm = form(Urls.class).bindFromRequest();
        return ok(index.render(User.findByEmail(request().username()),urlForm));
    }

    public Result addUrls(){
        Form<Urls> urlForm = form(Urls.class).bindFromRequest();
        String email = ctx().session().get("email");
        User user = User.findByEmail(email);
        if (urlForm.hasErrors()) {
            return badRequest(index.render(user,urlForm));
        }
        else{
            try {
                Dashboard.Urls dUrls = urlForm.get();
                final Pattern p = Pattern.compile(REGEX);
                String[] urlList = p.split(dUrls.url);
                for(String url:urlList) {
                    models.Urls urls = new models.Urls();
                    urls.url = url;
                    urls.save();
                }
                flash("success", Messages.get("added.blocking.urls"));
                return ok(index.render(user, urlForm));
            } catch (Exception e) {
                flash("failure", Messages.get("add.blocking.urls"));
                /*ArrayList<ValidationError> validationErrors = new ArrayList<>();
                validationErrors.add(urlForm.error(e.getMessage()));
                urlForm.errors().put(Messages.get("add.blocking.urls"), validationErrors);*/
                return badRequest(index.render(user, urlForm));
            }
        }
    }

    /**
     * Login class used by Login Form.
     */
    public static class Urls {

        @Constraints.Required
        public String url;

        /**
         * Validate the authentication.
         *
         * @return null if validation ok, string with details otherwise
         */
        public String validate() {
            HttpURLConnection connection = null;
            try{
                final Pattern p = Pattern.compile(REGEX);
                String[] urlList = p.split(url);
                for(String url: urlList) {
                    URL myUrl = new URL(url);
                    connection = (HttpURLConnection) myUrl.openConnection();
                    //Set request to header to reduce load
                    connection.setRequestMethod("HEAD");
                }
            } catch(Exception e){
                return "invalid url";
            }
            return null;
        }

        private boolean isBlank(String input) {
            return input == null || input.isEmpty() || input.trim().isEmpty();
        }

    }
}
