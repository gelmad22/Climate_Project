package at.kaindorf.climate_project;

import at.kaindorf.climate_project.db.InitDB;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MainController implements ApplicationRunner {

    private final InitDB initDB;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initDB.createDataFromFile();
    }
}
