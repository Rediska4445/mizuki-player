package ebanina.io.resource;

import javafx.scene.image.Image;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rf.ebanina.File.Resources.ResourceManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResourceManagerTest {

    private static final String TEST_PROPERTIES_FILE = "resources.properties";
    private ResourceManager rm;

    @BeforeEach
    void setUp() throws IOException {
        try (FileWriter writer = new FileWriter(TEST_PROPERTIES_FILE)) {
            writer.write("logPath=logs\n");
            writer.write("logErr=${logPath}/errors\n");
            writer.write("resourcePath=res\n");
            writer.write("modsPath=${resourcePath}/mods\n");
            writer.write("stylesheetPath=${resourcePath}/styles\n");
            writer.write("image=png\n");
            writer.write("logo.png=${resourcePath}/images/logo.png\n");
        }

        rm = new ResourceManager("res/resources.properties".replace("/", File.separator)) {
            {
                loadResources(TEST_PROPERTIES_FILE);
            }
        };
    }

    @AfterEach
    void tearDown() {
        new File(TEST_PROPERTIES_FILE).delete();
    }

    @Test
    void testResolveVariables() {
        assertEquals("logs/errors".replace("/", File.separator), rm.resourcesPaths.get("logErr"));
    }

    @Test
    void testLoadResourcesPaths() {
        assertEquals("res/styles".replace("/", File.separator), rm.resourcesPaths.get("stylesheetPath"));
        assertEquals("res/images/logo.png".replace("/", File.separator), rm.resourcesPaths.get("logo.png"));
    }

    @Test
    void testLoadResource_missingFileReturnsNull() {
        assertNull(rm.loadResource(Image.class,  "image", "image"));
    }

    @Test
    void testLoadResource_withNonexistentNameReturnsNull() {
        assertNull(rm.loadResource(Image.class, "image", "image"));
    }
}
