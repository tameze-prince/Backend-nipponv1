package prod.nipponhubv1.nipponhubv1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import prod.nipponhubv1.nipponhubv1.Configuration.DotenvConfig;

@SpringBootApplication
public class Nipponhubv1Application {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(Nipponhubv1Application.class);
        app.addInitializers(new DotenvConfig());	
        app.run(args);
	}

}
