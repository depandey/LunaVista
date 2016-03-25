package models;

import com.avaje.ebean.Model;
import models.User;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by 609108084 on 07/03/2016.
 */
public class Response {
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private Object data;
    private String message;

    public static class responseBuilder {
        private String status;
        private Object data;
        private String message;

        private responseBuilder() {
        }

        public static responseBuilder aresponse() {
            return new responseBuilder();
        }

        public responseBuilder withStatus(String status) {
            this.status = status;
            return this;
        }

        public responseBuilder withData(Object data) {
            if(data instanceof User.SignUpUserResponseBuilder){
                this.data = data;
                return this;
            }
            this.data = data;
            return this;
        }

        public responseBuilder withMessage(String message) {
            this.message = message;
            return this;
        }

        public responseBuilder but() {
            return aresponse().withStatus(status).withData(data).withMessage(message);
        }

        public Response build() {
            Response response = new Response();
            response.setStatus(status);
            response.setData(data);
            response.setMessage(message);
            return response;
        }
    }
}
