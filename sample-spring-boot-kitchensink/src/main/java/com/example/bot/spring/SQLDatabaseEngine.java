package com.example.bot.spring;

import lombok.extern.slf4j.Slf4j;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.net.URISyntaxException;
import java.net.URI;

@Slf4j
public class SQLDatabaseEngine extends DatabaseEngine {
	@Override
	String search(String userInput) throws Exception {
		// Write your code here
		String output = null;
		log.info("Input: " + userInput);
		Connection connection = getConnection();

		PreparedStatement stmtFindLongestMatch = connection
				.prepareStatement("SELECT output FROM chatbotresponse WHERE POSITION(LOWER(input) IN LOWER(?))>0 "
						+ "ORDER BY CHAR_LENGTH(input) desc;");
		stmtFindLongestMatch.setString(1, userInput);
		ResultSet rs = stmtFindLongestMatch.executeQuery();
		log.info("Success on executing query!");

		if (rs.next()) {
			output = rs.getString(1);
			log.info("Found the output!: " + output);
		}
		rs.close();
		stmtFindLongestMatch.close();

		if (output != null) {
			PreparedStatement stmtIncrementHits = connection
					.prepareStatement("UPDATE chatbotresponse SET hitcount=hitcount+1 WHERE output=?");
			stmtIncrementHits.setString(1, output);
			stmtIncrementHits.executeUpdate(); // it's an update, not a query!
			stmtIncrementHits.close();
			log.info("Success incrementing hitcount!");

			PreparedStatement stmtGetNewHits = connection
					.prepareStatement("SELECT hitcount FROM chatbotresponse WHERE output=?");
			stmtGetNewHits.setString(1, output);
			ResultSet rsHits = stmtGetNewHits.executeQuery();
			int hitcount = -1;
			if (rsHits.next()) {
				hitcount = rsHits.getInt(1);
				log.info("Success getting new hitcount!");
			}

			rsHits.close();
			stmtGetNewHits.close();
			connection.close();
			log.info("Returning: " + output + " (" + hitcount + " hit(s))");
			return output + " (" + hitcount + " hit(s))";
		} else {
			log.info("Entry in DB not found");
			connection.close();
			throw new Exception("NOT FOUND!");
		}
		// PreparedStatement stmtGetAll = connection.prepareStatement("SELECT input,
		// output FROM chatbotresponse;");
		// ResultSet rs = stmtGetAll.executeQuery();
		//
		// log.info("Success on executing query!");
		//
		// ArrayList<String[]> pairs = new ArrayList<String[]>();
		//
		// while (rs.next()) {
		// String[] temp = new String[] { rs.getString(1), rs.getString(2) };
		// pairs.add(temp);
		// // 1 index, not 0 index!!!
		// log.info("retrieved line: " + temp[0] + ", " + temp[1]);
		// }
		// rs.close();
		// stmtGetAll.close();
		//
		// log.info("done retrieving lines");
		// for (String[] pair : pairs) {
		// log.info(pair[0] + " + " + pair[1]);
		// }
		//
		// Comparator<String[]> lengthComp = new Comparator<String[]>() {
		// @Override
		// public int compare(String[] o1, String[] o2) {
		// if (o1[0].length() > o2[0].length())
		// return -1;
		// if (o2[0].length() > o1[0].length())
		// return 1;
		// return 0;
		// }
		// };
		//
		// pairs.sort(lengthComp);
		//
		// for (String[] pair : pairs) {
		// String check_input = pair[0];
		// if (userInput.toLowerCase().contains(check_input.toLowerCase())) {
		// log.info("trying to search for output " + pair[1]);
		// PreparedStatement incrementHits = connection.prepareStatement(
		// "UPDATE chatbotresponse SET hitcount=hitcount+1 WHERE output=?");
		// incrementHits.setString(1, ""+pair[1]);
		// incrementHits.executeUpdate(); // it's an update!
		// log.info("incremented hits!");
		//
		// incrementHits.close();
		//
		// PreparedStatement getNewHits = connection
		// .prepareStatement("SELECT hitcount FROM chatbotresponse WHERE output=?");
		// getNewHits.setString(1, ""+pair[1]);
		// ResultSet hits = getNewHits.executeQuery();
		//
		// int amt = -1;
		// while (hits.next()) {
		// amt = hits.getInt(1);
		// }
		// log.info("got hits! (" + amt + ")");
		//
		// hits.close();
		// getNewHits.close();
		// connection.close();
		// return pair[1] + " (" + amt + " hit(s))";
		// }
		// }
		// connection.close();
		// throw new Exception("NOT FOUND");
	}

	private Connection getConnection() throws URISyntaxException, SQLException {
		Connection connection;
		URI dbUri = new URI(System.getenv("DATABASE_URL"));

		String username = dbUri.getUserInfo().split(":")[0];
		String password = dbUri.getUserInfo().split(":")[1];
		String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath()
				+ "?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";

		log.info("Username: {} Password: {}", username, password);
		log.info("dbUrl: {}", dbUrl);

		connection = DriverManager.getConnection(dbUrl, username, password);

		log.info("Success on getting a connection!");
		return connection;
	}

}
