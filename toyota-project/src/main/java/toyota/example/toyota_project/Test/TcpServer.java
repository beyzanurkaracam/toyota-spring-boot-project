package toyota.example.toyota_project.Test;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8081)) {
            System.out.println("TCP Server başlatıldı, port: 8081");
            
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client bağlandı!");
            
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            
            // Test verisi gönder
            String testData = "PF1_USDTRY|22:number:3.5000|25:number:3.5100|timestamp:2024-02-14T10:00:00Z";
            out.println(testData);
            System.out.println("Test verisi gönderildi: " + testData);
            
            Thread.sleep(2000);
            
            // Güncelleme verisi gönder
            String updateData = "PF1_USDTRY|22:number:3.5200|25:number:3.5300|timestamp:2024-02-14T10:00:05Z";
            out.println(updateData);
            System.out.println("Güncelleme verisi gönderildi: " + updateData);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
