package com.liaconcept.radar;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ConsoleApplication extends Application {

	ConsoleController consoleController;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("Console.fxml"));
		Parent root = loader.load();
		consoleController = loader.getController();
		primaryStage.setTitle("Console CM02");
		primaryStage.setScene(new Scene(root, 700, 450));
		primaryStage.show();
	}

	@Override
	public void stop() {
		System.out.println("ConsoleCM02 is closing...");
		consoleController.shutdown();
	}
}
