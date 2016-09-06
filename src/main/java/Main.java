import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import model_canvas.Location;
import utility.DropShadowHelper;

public class Main extends Application {

    private final MouseTracker mouseTracker = new MouseTracker();

    private static Location locationOnMouse = null;

    public static void main(String[] args) {
        launch(args);
    }

    // Update the position of the new location when the mouse moved
    private final EventHandler<MouseEvent> mouseMovedEventHandler = mouseMovedEvent -> {
        locationOnMouse.setCenterX(mouseMovedEvent.getX());
        locationOnMouse.setCenterY(mouseMovedEvent.getY());
    };

    // Place the new location when the mouse is pressed (i.e. stop moving it)
    private final EventHandler<MouseEvent> mouseClickedEventHandler = mouseClickedEvent -> {
        if (locationOnMouse != null) {
            Animation locationPlaceAnimation = new Transition() {
                {
                    setCycleDuration(Duration.millis(50));
                }

                protected void interpolate(double frac) {
                    locationOnMouse.setEffect(DropShadowHelper.generateElevationShadow(12 - 12 * frac));
                }
            };
            locationPlaceAnimation.play();

            locationPlaceAnimation.setOnFinished(event -> {
                locationOnMouse = null;
                mouseTracker.unregisterOnMouseMovedEventHandler(mouseMovedEventHandler);
            });
        }
    };

    public void start(final Stage stage) throws Exception {
        mouseTracker.registerOnMouseClickedEventHandler(mouseClickedEventHandler);

        stage.setTitle("Kick-ass Modelchecker");

        // Create the root pane of the window and register mouse event listeners
        final AnchorPane root = new AnchorPane();
        root.setOnMouseMoved(mouseTracker.onMouseMovedEventHandler);
        root.setOnMouseClicked(mouseTracker.onMouseClickedEventHandler);

        final Scene scene = new Scene(root, 1000, 1000);

        scene.getStylesheets().add("colors.css");
        scene.getStylesheets().add("model_canvas/location.css");
        stage.setScene(scene);

        // Whenever the L key is pressed, create a new location following the cursor
        scene.setOnKeyPressed(event -> {
            if (!event.getCode().equals(KeyCode.L)) return;

            if (locationOnMouse == null) {
                locationOnMouse = new Location(mouseTracker.getX(), mouseTracker.getY());
                locationOnMouse.setEffect(DropShadowHelper.generateElevationShadow(22));
                root.getChildren().add(locationOnMouse);

                mouseTracker.registerOnMouseMovedEventHandler(mouseMovedEventHandler);
            }
        });

        stage.show();
    }
}
