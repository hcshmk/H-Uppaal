package dk.cs.aau.huppaal.controllers;

import dk.cs.aau.huppaal.HUPPAAL;
import dk.cs.aau.huppaal.abstractions.Component;
import dk.cs.aau.huppaal.presentations.DropDownMenu;
import dk.cs.aau.huppaal.presentations.FilePresentation;
import dk.cs.aau.huppaal.utility.UndoRedoStack;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXTextArea;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

public class ProjectPaneController implements Initializable {

    private final HashMap<Component, FilePresentation> componentPresentationMap = new HashMap<>();
    public StackPane root;
    public AnchorPane toolbar;
    public Label toolbarTitle;
    public ScrollPane scrollPane;
    public VBox filesList;
    public JFXRippler createComponent;
    public VBox mainComponentContainer;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        HUPPAAL.getProject().getComponents().addListener(new ListChangeListener<Component>() {
            @Override
            public void onChanged(final Change<? extends Component> c) {
                while (c.next()) {
                    c.getAddedSubList().forEach(o -> handleAddedComponent(o));
                    c.getRemoved().forEach(o -> handleRemovedComponent(o));

                    // We should make a new component active
                    if (c.getRemoved().size() > 0) {
                        if (HUPPAAL.getProject().getComponents().size() > 0) {
                            // Find the first available component and show it instead of the removed one
                            final Component component = HUPPAAL.getProject().getComponents().get(0);
                            CanvasController.setActiveComponent(component);
                        } else {
                            // Show no components (since there are none in the project)
                            CanvasController.setActiveComponent(null);
                        }
                    }

                    // Sort the children alphabetically
                    sortPresentations();
                }
            }
        });

        HUPPAAL.getProject().getComponents().forEach(this::handleAddedComponent);
    }

    private void sortPresentations() {
        final ArrayList<Component> sortedComponentList = new ArrayList<>();
        componentPresentationMap.keySet().forEach(sortedComponentList::add);
        sortedComponentList.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
        sortedComponentList.forEach(component -> componentPresentationMap.get(component).toFront());
    }

    private void initializeColorSelector(final FilePresentation filePresentation) {
        final JFXRippler moreInformation = (JFXRippler) filePresentation.lookup("#moreInformation");
        final int listWidth = 230;
        final DropDownMenu moreInformationDropDown = new DropDownMenu(moreInformation, listWidth, true);
        final Component component = filePresentation.getComponent();

        moreInformationDropDown.addListElement("Configuration");

        /*
         * IS MAIN
         */
        moreInformationDropDown.addTogglableListElement("Main", filePresentation.getComponent().isMainProperty(), event -> {
            //Check if the component is already true (added to avoid having no main component)
            if(!component.isIsMain()){
                final boolean wasMain = component.isIsMain();

                UndoRedoStack.push(() -> { // Perform
                    component.setIsMain(!wasMain);
                }, () -> { // Undo
                    component.setIsMain(wasMain);
                }, "Component " + component.getName() + " isMain: " + !wasMain, "star");
            } else {
                //Inform the user that it is not possible to have no main component
                HUPPAAL.showToast("To change the main component, please set another component as main");
            }
        });

        /*
         * INCLUDE IN PERIODIC CHECK
         */
        moreInformationDropDown.addTogglableListElement("Include in periodic check", component.includeInPeriodicCheckProperty(), event -> {
            final boolean didIncludeInPeriodicCheck = component.includeInPeriodicCheckProperty().get();

            UndoRedoStack.push(() -> { // Perform
                component.includeInPeriodicCheckProperty().set(!didIncludeInPeriodicCheck);
            }, () -> { // Undo
                component.includeInPeriodicCheckProperty().set(didIncludeInPeriodicCheck);
            }, "Component " + component.getName() + " is included in periodic check: " + !didIncludeInPeriodicCheck, "search");
        });

        moreInformationDropDown.addSpacerElement();

        moreInformationDropDown.addListElement("Description");

        final JFXTextArea textArea = new JFXTextArea();
        textArea.setMinHeight(30);

        filePresentation.getComponent().descriptionProperty().bindBidirectional(textArea.textProperty());

        textArea.textProperty().addListener((obs, oldText, newText) -> {
            int i = 0;
            for (final char c : newText.toCharArray()) {
                if (c == '\n') {
                    i++;
                }
            }

            textArea.setMinHeight(i * 17 + 30);
        });

        moreInformationDropDown.addCustomChild(textArea);

        moreInformationDropDown.addSpacerElement();

        moreInformationDropDown.addListElement("Color");

        /*
         * COLOR SELECTOR
         */
        moreInformationDropDown.addColorPicker(filePresentation.getComponent(), filePresentation.getComponent()::color);

        moreInformationDropDown.addSpacerElement();

        /*
         * THE DELETE BUTTON
         */
        moreInformationDropDown.addClickableListElement("Delete", event -> {
            UndoRedoStack.push(() -> { // Perform
                HUPPAAL.getProject().getComponents().remove(component);
            }, () -> { // Undo
                HUPPAAL.getProject().getComponents().add(component);
            }, "Deleted component " + component.getName(), "delete");

            //Check if the component to be removed is the main component and that there exists another component
            if(component.isIsMain() && !componentPresentationMap.isEmpty()) {
                //Set another component as the main component
                componentPresentationMap.keySet().iterator().next().setIsMain(true);
            }

            moreInformationDropDown.close();
        });

        moreInformation.setOnMousePressed((e) -> {
            e.consume();
            moreInformationDropDown.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 10, 10);
        });
    }

    private void handleAddedComponent(final Component component) {
        final FilePresentation filePresentation = new FilePresentation(component);
        initializeColorSelector(filePresentation);
        filesList.getChildren().add(filePresentation);
        componentPresentationMap.put(component, filePresentation);

        // Open the component if the presentation is pressed
        filePresentation.setOnMousePressed(event -> {
            event.consume();
            CanvasController.setActiveComponent(component);
        });

        component.nameProperty().addListener(obs -> sortPresentations());

        component.isMainProperty().addListener((obs, oldIsMain, newIsMain) -> {
            final Component mainComponent = HUPPAAL.getProject().getMainComponent();

            if (component.equals(mainComponent) && !newIsMain) {
                HUPPAAL.getProject().setMainComponent(null);
                return;
            }

            if (mainComponent != null && newIsMain) {
                mainComponent.setIsMain(false);
            }

            HUPPAAL.getProject().setMainComponent(component);
        });
    }

    private void handleRemovedComponent(final Component component) {
        filesList.getChildren().remove(componentPresentationMap.get(component));
        componentPresentationMap.remove(component);
    }

    @FXML
    private void createComponentClicked() {
        final Component newComponent = new Component(true);

        UndoRedoStack.push(() -> { // Perform
            HUPPAAL.getProject().getComponents().add(newComponent);
        }, () -> { // Undo
            HUPPAAL.getProject().getComponents().remove(newComponent);
        }, "Created new component: " + newComponent.getName(), "add-circle");

        CanvasController.setActiveComponent(newComponent);
    }

}
