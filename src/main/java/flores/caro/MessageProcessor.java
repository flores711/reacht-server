package flores.caro;

import flores.caro.model.entities.DataPackage;
import flores.caro.utils.DBDAO;


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


        return new DataPackage("s", null);
    }


}
