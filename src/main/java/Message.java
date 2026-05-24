import java.time.LocalTime;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Message {

    public enum Type {
        Public,
        Private,
        Error
    }

    public Type type;
    public String sender;
    public String time;
    public String body;
    public String recipient;

    public Message(
            Type type,
            String sender,
            String recipient,
            String body
    ) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.body = body;
        this.time = LocalTime.now().toString();
    }

    @SuppressWarnings("unchecked")
    public String toJson() {

        JSONObject obj = new JSONObject();

        obj.put("type", type.name());
        obj.put("sender", sender);
        obj.put("time", time);
        obj.put("body", body);

        if (recipient != null) {
            obj.put("recipient", recipient);
        }

        return obj.toJSONString();
    }

    public static Message fromJson(String json)
            throws Exception {

        JSONParser parser = new JSONParser();

        JSONObject obj =
                (JSONObject) parser.parse(json);

        Type type =
                Type.valueOf(
                        (String) obj.get("type")
                );

        String sender =
                (String) obj.get("sender");

        String recipient =
                (String) obj.get("recipient");

        String body =
                (String) obj.get("body");

        Message msg =
                new Message(
                        type,
                        sender,
                        recipient,
                        body
                );

        msg.time =
                (String) obj.get("time");

        return msg;
    }
}