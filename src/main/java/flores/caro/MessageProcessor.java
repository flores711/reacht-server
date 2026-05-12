package flores.caro;

import flores.caro.model.DataPackage;
import flores.caro.utils.DBDAO;
import tools.jackson.databind.ObjectMapper;


public class MessageProcessor {
    private DBDAO dao;

    public MessageProcessor(DBDAO dao) {
        this.dao = dao;
    }

    public String processMessage(String json) {
        DataPackage dataPackage =   jsonToPackage(json);
        switch (dataPackage.getAction()) {
            case "LOGIN":
                logIn(dataPackage);
                break;




        }

        return "json";
    }

    private boolean logIn(DataPackage dataPackage) {
        try {
            String username = (String) dataPackage.getData().get("username");
            String password = (String) dataPackage.getData().get("password");

            // TODO: this has to be a separate thread
            return dao.checkCredentials(username, password);

        } catch (ClassCastException e) {
            System.err.println("CLASS CAST EXCEPTION ON LOG IN");
            return false;
        }
    }






    public DataPackage jsonToPackage(String json) {
        // Ver última respuesta Gemini explicando esto con Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        DataPackage dataPackage;

        try {
            dataPackage = objectMapper.readValue(json, DataPackage.class);
            System.out.println(dataPackage.getAction());
            System.out.println(dataPackage.getData().get("username"));
            System.out.println(dataPackage.getData().get("password"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return dataPackage;


        /*
        Prompt Gemini:
        2. La clase DataPackage

Jackson es un poco estricto: necesita un constructor vacío (el que viene por defecto si no pones ninguno) y los getters/setters para poder trabajar, a no ser que le pongas anotaciones. Déjala así de limpia:



Java



import java.util.Map;public class DataPackage {

    private String action;

    private Map<String, Object> data;



    // Constructor vacío obligatorio para Jackson (si creas otros constructores, pon este explícitamente)

    public DataPackage() {}



    // Getters

    public String getAction() { return action; }

    public Map<String, Object> getData() { return data; }



    // Setters

    public void setAction(String action) { this.action = action; }

    public void setData(Map<String, Object> data) { this.data = data; }

}





es necesario los setters para que funcione? yo lo he hecho sin setters ni anotaciones y me ha funcionado bien el objectmapper
         */
    }


}
