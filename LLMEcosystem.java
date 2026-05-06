/**
 * 	Authors: 
 * 			Luis Miranda			mirandaquijada@arizona.edu
 * 			Shale Van Cleve			shalevancleve@arizona.edu
 * 			Zoltan Kotha			zkotha@arizona.edu
 * 			Ian Sanchez Lopez		iansanchezl@arizona.edu
 * 	Course: 
 * 			CSC460 Spring 2026
 * 	Assignment: 
 * 			Program #4: Database Design and Implementation
 * 	Instructors/TAs: 
 * 			Lester I. McCann, Jianwei Shen, Muhammad Bilal
 * 	Due Date: 
 * 			5/5/2026 2:00 PM
 * 	Problem Desc: 
 * 			Database-driven information management system for an LLM User-Facing Ecosystem, using JDBC
 * 			as a front-end for the user to interact with SQL and the Oracle DBMS on aloe.cs.arizona.edu
 * 	Additional Solution info:
 * 			Primarily uses the PreparedStatement object and its associated methods to set up SQL queries 
  			and insert user-provided parameters into the appropriate queries to be run. 
 * 	Operational Requirements:
 *          Java 8+, Oracle JDBC driver on classpath
 *  Usage: 
 * 			java LLMEcosystem
 */

import java.sql.*;
import java.util.Scanner;

public class LLMEcosystem
{

	private static final String DB_URL = "jdbc:oracle:thin:@aloe.cs.arizona.edu:1521:oracle";
	private static final String DB_USER = "shalevancleve";
	private static final String DB_PASS = "a2532";

	private Connection conn;
	private Scanner scanner;

	public LLMEcosystem()
	{
		scanner = new Scanner(System.in);
		connectToDatabase();
	}

	private void connectToDatabase()
	{
		try
		{
			Class.forName("oracle.jdbc.OracleDriver");
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
			System.out.println("Connected to Oracle DB successfully.\n");
		}
		catch (Exception e)
		{
			System.err.println("Database connection error: " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * ====================================================== Manage user accounts
	 * -- functionality 1 addUser() updateUser() deleteUser()
	 */

	/*
	 * Adds a new user to the User table
	 *
	 * @param name, the name of the new user to be added
	 * 
	 * @param email, the email address of a user to be added
	 * 
	 * @param preferredUI, the UI language of the LLM
	 * 
	 * @return None
	 * 
	 * @note when creating user. tier defaults to "Free" with tierID 1
	 */
	public void addUser(String name, String email, String preferredUI)
	{
		String insertStmt = "INSERT INTO Users (UserID, UName, Email, PreferredUI, DateCreated, TierID) "
				+ "VALUES (user_seq.NEXTVAL, ?, ?, ?, SYSDATE, 1)";
		try
		{
			PreparedStatement stmt = conn.prepareStatement(insertStmt);
			stmt.setString(1, name);
			stmt.setString(2, email);
			stmt.setString(3, preferredUI);
			stmt.executeUpdate();
		}
		catch (SQLException e)
		{
			System.err.println("Error adding user to database");
		}
	}

	/*
	 * Updates a user's info in the database.
	 *
	 * @param UserID, the unique id of the user being updated
	 * 
	 * @param newName, the (maybe) updated name of a user to insert
	 * 
	 * @param newEmail, the (maybe) new email address of a user to insert
	 * 
	 * @param preferredUI, the (maybe) new preferred language of the AI for a user
	 * 
	 * @param tierID, the (maybe) new model tier a user wants to subscribe to
	 * 
	 * @return None
	 * 
	 * @note dateCreated & UserID fields remain unchanged
	 */
	public void updateUser(int UserID, String newName, String newEmail, String newPreferredUI, int newTierID)
			throws SQLException
	{
		String updateQuery = "UPDATE Users SET UName = ?, Email = ?, PreferredUI = ?, TierID = ? WHERE UserID = ?";
		PreparedStatement stmt = conn.prepareStatement(updateQuery);
		stmt.setString(1, newName);
		stmt.setString(2, newEmail);
		stmt.setString(3, newPreferredUI);
		stmt.setInt(4, newTierID);
		stmt.setInt(5, UserID);
		stmt.executeUpdate();
	}

	/*
	 * 
	 * Checks if a user has unpaid invoices or an unclosed support ticket
	 *
	 * @param UserID, the unique id of the user whose invoices/tickets we are
	 * checking
	 * 
	 * @return True if the user has unpaid invoices or unclosed tickets, else false
	 */
	public boolean checkUnpaidInvoicesOrSupportTickets(int UserID) throws SQLException
	{
		String query = "SELECT DISTINCT UserID FROM SupportTicket WHERE UserID = ? AND DateClosed IS NULL" + " UNION "
				+ "SELECT DISTINCT UserID FROM Invoice WHERE UserID = ? AND IStatus = 'unpaid'";
		PreparedStatement stmt = conn.prepareStatement(query);
		stmt.setInt(1, UserID);
		stmt.setInt(2, UserID);
		ResultSet rs = stmt.executeQuery();
		return rs.next(); // if there is a next, it means userID was found in either condition
	}

	/*
	 * Deletes a user in the database, and their associated info.
	 *
	 * @param UserID, the unique id of the user whose record we are deleting
	 * 
	 * @return None
	 * 
	 * @note deletion fails if invoices unpaid or open support tickets
	 */
	public void deleteUser(int UserID) throws SQLException
	{
		boolean failed = checkUnpaidInvoicesOrSupportTickets(UserID);
		if (failed)
		{
			System.out.println("Cannot delete user account: invoice is unpaid or a support ticket is open, or user does not exist.");
			return;
		}
		String deleteStmt = "DELETE FROM Users WHERE UserID = ?"; // sql "ON DELETE CASCADE" in table creation will
																	// delete user's messages and conversations
		PreparedStatement stmt = conn.prepareStatement(deleteStmt);
		stmt.setInt(1, UserID);
		stmt.executeUpdate();
		System.out.printf("User with ID %d deleted successfully", UserID);
	}

	/**
	 * ====================================================== Handle Conversations &
	 * messages -- functionality 2 startConversation() updateMessageFeedback() addMessageToConversation()
	 */

	/*
	 * Starts a new conversation in the LLM database, optionally adding a persona to
	 * it. If personas are unavailable (none created yet) the conversation will be
	 * initialized with NULL persona IDs
	 *
	 * @param None
	 * 
	 * @return None
	 */
	private void startConversation() throws SQLException
	{

		System.out.println("Please enter UserID: "); // id of user starting conversation
		int uID = Integer.parseInt(scanner.nextLine().trim());

		System.out.println("Please enter conversation title: ");
		String cTitle = scanner.nextLine().trim();

		// when a user starts a conversation, they can attach a persona to it
		System.out.println("Available personas: ");
		String pQuery = "SELECT PersonaID, PName, VersionID FROM Persona WHERE DeletedStatus = 0"; // only select active
																									// personas
		Statement stmt1 = conn.createStatement();
		ResultSet result = stmt1.executeQuery(pQuery);
		boolean personasAvailable = false;
		while (result.next())
		{
			personasAvailable = true;
			System.out.println("Persona ID: " + result.getInt("PersonaID") + "\nName: " + result.getString("PName")
					+ "\nVersionID: " + result.getInt("VersionID"));
		}
		System.out.println("Would you like to add a persona to this conversation? (y/n)");
		String answer = scanner.nextLine();

		if ((answer.equals("y") && !personasAvailable) || answer.equals("n"))
		{
			System.out.println("No personas selected or none available. Adding conversation...\n");
			String insQuery = "INSERT INTO Conversation (ConversationID, Title, DateCreated, VersionID, PersonaID, UserID, WorkspaceID, IsActive) "
					+ "VALUES (conv_seq.NEXTVAL, ?, SYSDATE, NULL, NULL, ?, NULL, 1)";
			PreparedStatement stmt2 = conn.prepareStatement(insQuery);
			stmt2.setString(1, cTitle);
			stmt2.setInt(2, uID);
			stmt2.executeUpdate();
		}
		else if (answer.equals("y") && personasAvailable)
		{
			System.out.println("Input <personaID> <versionID> of persona you'd like:\n");
			int pID = Integer.parseInt(scanner.nextLine().trim());
			int vID = Integer.parseInt(scanner.nextLine().trim());
			String insQuery = "INSERT INTO Conversation (ConversationID, Title, DateCreated, VersionID, PersonaID, UserID, WorkspaceID, IsActive) "
					+ "VALUES (conv_seq.NEXTVAL, ?, SYSDATE, ?, ?, ?, NULL, 1)";
			PreparedStatement stmt2 = conn.prepareStatement(insQuery);
			stmt2.setString(1, cTitle);
			stmt2.setInt(2, vID);
			stmt2.setInt(3, pID);
			stmt2.setInt(4, uID);
			stmt2.executeUpdate();
		}
		else
		{
			System.out.println("Must select y or n. Conversation failed to be created.");
		}
	}

	/*
	 * Adds or updates feedback to a message, depending on if a feedback relation
	 * already exists for a requested message.
	 *
	 * @param None
	 * 
	 * @return None
	 */
	private void updateMessageFeedback() throws SQLException
	{
		System.out.println("Please enter userID to see messages: ");
		int uID = Integer.parseInt(scanner.nextLine().trim());

		System.out.println("Available messages to give feedback to: ");
		// only AI messages can receive feedback
		String msgQuery = "SELECT MessageID, Content FROM Conversation c JOIN Message m ON c.ConversationID = m.ConversationID WHERE c.UserID = ? AND m.SenderRole = 'AI'"; 
																									
		PreparedStatement stmt = conn.prepareStatement(msgQuery);
		stmt.setInt(1, uID);
		ResultSet rs = stmt.executeQuery();

		while (rs.next())
		{
			System.out.println("MessageID: " + rs.getInt("MessageID") + " | Content: " + rs.getString("Content"));
		}

		System.out.println("Enter the MessageID you want to give feedback to: ");
		int msgID = Integer.parseInt(scanner.nextLine().trim());

		System.out.println("Enter feedback text: ");
		String feedback = scanner.nextLine();

		System.out.println("Enter if message good or bad (1/0): ");
		int rating = Integer.parseInt(scanner.nextLine().trim());

		String chkQuery = "SELECT COUNT(*) FROM Feedback WHERE MessageID = ?"; // are there any feedback relations for
																				// the message?
		PreparedStatement chkStmt = conn.prepareStatement(chkQuery);
		chkStmt.setInt(1, msgID);
		ResultSet chkRs = chkStmt.executeQuery();
		chkRs.next();
		int count = chkRs.getInt(1);

		if (count > 0)
		{ // feedback exists, just modify it
			String updateQuery = "UPDATE Feedback SET FText = ?, IsThumbsUp = ? WHERE MessageID = ?";
			PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
			updateStmt.setString(1, feedback);
			updateStmt.setInt(2, rating);
			updateStmt.setInt(3, msgID);
			updateStmt.executeUpdate();
		}
		else
		{ // message has no feedback, insert new feedback into database
			String insQuery = "INSERT INTO Feedback (FeedbackID, IsThumbsUp, FText, FDate, UserID, MessageID) "
					+ "VALUES (feedback_seq.NEXTVAL, ?, ?, SYSDATE, ?, ?)";
			PreparedStatement insStmt = conn.prepareStatement(insQuery);
			insStmt.setInt(1, rating);
			insStmt.setString(2, feedback);
			insStmt.setInt(3, uID);
			insStmt.setInt(4, msgID);
			insStmt.executeUpdate();
		}
	}

	/*
	 * Bookmarks a user's message for quick retrieval, by adding it to a
	 * User-Message Relational table
	 *
	 * @param None
	 * 
	 * @return None
	 */
	private void bookmarkMessage() throws SQLException
	{
		System.out.print("Enter UserID: ");
		int uId = Integer.parseInt(scanner.nextLine().trim());
		System.out.print("Enter MessageID to bookmark: ");
		int msgId = Integer.parseInt(scanner.nextLine().trim());

		String checkSql = "SELECT COUNT(*) FROM Message m "
				+ "JOIN Conversation c ON m.ConversationID = c.ConversationID "
				+ "WHERE m.MessageID = ? AND c.UserID = ?"; // makes sure message belongs to user
		PreparedStatement checkStmt = conn.prepareStatement(checkSql);
		checkStmt.setInt(1, msgId);
		checkStmt.setInt(2, uId);
		ResultSet rs = checkStmt.executeQuery();
		rs.next();
		if (rs.getInt(1) == 0)
		{
			System.out.println("Message not found or does not belong to this user.");
			return;
		}

		String bookmarkSql = "INSERT INTO UserBookmarks (UserID, MessageID) VALUES (?, ?)";
		try (PreparedStatement stmt = conn.prepareStatement(bookmarkSql))
		{
			stmt.setInt(1, uId);
			stmt.setInt(2, msgId);
			stmt.executeUpdate();
			System.out.println("Message bookmarked successfully.");
		}
	}

	private void addMessageToConversation() throws SQLException
	{
		System.out.print("UserID: ");
		int uid = Integer.parseInt(scanner.nextLine().trim());
		System.out.print("ConversationID: ");
		int cid = Integer.parseInt(scanner.nextLine().trim());
		System.out.print("SenderRole (User/AI): ");
		String role = scanner.nextLine().trim();
		if (!role.equals("User") && !role.equals("AI"))
		{
			System.out.println("SenderRole must be 'User' or 'AI'.");
			return;
		}
		System.out.print("Message content: ");
		String content = scanner.nextLine().trim();
		int msgId = insertMessageIfWithinLimit(uid, cid, role, content);
		if (msgId == -1)
		{
			System.out.println("Daily message limit exceeded. Message rejected.");
		}
		else
		{
			System.out.println("Message added with ID: " + msgId);
		}
	}

	/**
	 * ====================================================== workspace organization
	 * -- functionality 3
	 */

	/*
	 * getAvailableWorkspaceID() Method designed to get the next available
	 * workspaceID from the Oracle sequence. Precondition: the connection is active
	 * workspace_seq is already in the db. Postcondition: it gets a new ID. Return
	 * values: the next available ID, or -1 if not possible to get one.
	 */
	private int getAvailableWorkspaceID()
	{
		// get next ID from sequence
		String sq = "SELECT workspace_seq.NEXTVAL from dual";
		try
		{
			Statement s = conn.createStatement();
			ResultSet r = s.executeQuery(sq);

			// get the number
			if (r.next())
			{
				int nexID = r.getInt(1);
				r.close();
				s.close();
				return nexID;
			}

			r.close();
			s.close();
		}
		catch (SQLException e)
		{
			System.err.println("Error getting next available WorkspaceID");
			System.err.println(e.getMessage());
		}

		return -1;
	}

	/*
	 * createWorkspace() Method designed to create a Workspace. The creator is added
	 * as Admin. Precondition: the connection is active workspace_seq is already in
	 * the db. UserID of creator exists in table User.
	 * 
	 * Postcondition: new rows marching are inserted into Workspace and
	 * UserWorkspace. Transaction is rolled back if either insert fails.
	 * 
	 * Return values: None
	 */
	private void createWorkspace()
	{
		try
		{
			System.out.println("Enter userID of the creator:");
			int userID = Integer.parseInt(scanner.nextLine().trim());

			System.out.println("Enter Workspace name:");
			String name = scanner.nextLine().trim();

			System.out.println("Enter Visibility:");
			String visibility = scanner.nextLine().trim();

			if (!visibility.equals("shared") && !visibility.equals("private"))
			{
				System.out.println("Options for Visibility are: shared, private");

				return;
			}
			int workspaceID = getAvailableWorkspaceID();

			if (workspaceID == -1)
			{
				System.out.println("Something went wrong creating workspace as workspaceID " + "was not generated");

				return;
			}

			// we'll do two inserts, so either both work or none do.
			conn.setAutoCommit(false);

			// first one
			String insertWorkspace = "INSERT INTO Workspace (WorkspaceID, WName, Visibility, DateCreated) "
					+ "VALUES(?, ?, ?, CURRENT_DATE)";
			PreparedStatement st1 = conn.prepareStatement(insertWorkspace);

			// ? workspaceID
			st1.setInt(1, workspaceID);

			// ? name
			st1.setString(2, name);

			// ? visibility
			st1.setString(3, visibility);

			st1.executeUpdate();

			st1.close();

			// second one
			String insertUserWorkspace = "INSERT INTO UserWorkspace (UserID, WorkspaceID, DateJoined, UWRole) "
					+ "VALUES(?, ?, SYSDATE, 'Admin')";

			PreparedStatement st2 = conn.prepareStatement(insertUserWorkspace);

			// ? userID
			st2.setInt(1, userID);

			// ? workspaceID
			st2.setInt(2, workspaceID);

			st2.executeUpdate();
			st2.close();

			conn.commit();

			// reactivate autocomit
			conn.setAutoCommit(true);

			System.out.println("Workspace created. WorkspaceID: " + workspaceID);
			System.out.println("UserID: " + userID);

			// user did not use numbers
		}
		catch (NumberFormatException e)
		{
			System.out.println("IDs must be integers");

		}
		catch (SQLException e)
		{

			try
			{
				// an insert did not work, so go back in the changes
				conn.rollback();

				conn.setAutoCommit(true);
			}

			catch (SQLException rollingbackE)
			{
				System.err.println("Error rolling back.");
			}

			System.err.println("Error creating the workspace.");
			System.err.println(e.getMessage());
		}
	}

	/*
	 * modifyWorkspace() Method designed to update the name or visibility of an
	 * existing workspace. Precondition: the connection to the db is active
	 *
	 * Postcondition: If workspaceID given by user is found, it is updated Return
	 * values: None
	 */
	private void modifyWorkspace()
	{
		try
		{
			System.out.println("Enter WorkspaceId to modify:");
			int wsID = Integer.parseInt(scanner.nextLine().trim());

			System.out.println("Enter the new Workspace name:");
			String newName = scanner.nextLine().trim();

			System.out.println("Enter the new Visibility:");
			String newV = scanner.nextLine().trim();

			if (!newV.equals("shared") && !newV.equals("private"))
			{
				System.out.println("Options for Visibility are: shared, private");
				return;
			}

			String updateWorkspace = "UPDATE Workspace " + "SET WName = ?, Visibility = ? " + "WHERE WorkspaceID = ?";

			PreparedStatement st = conn.prepareStatement(updateWorkspace);

			// ? = new name
			st.setString(1, newName);

			// ? = new Visibility
			st.setString(2, newV);

			// ? = workspaceId to modify
			st.setInt(3, wsID);

			int rowschanged = st.executeUpdate();

			st.close();
			// if something was changed, it modified workspace
			if (rowschanged > 0)
			{
				System.out.println("Workspace modified.");
			}
			else
			{
				System.out.println("Workspace not found.");
			}

		}
		catch (NumberFormatException e)
		{
			System.out.println("workspaceID must be an integer.");

		}
		catch (SQLException e)
		{
			System.err.println("Something went wrong modifying the workspace.");
			System.err.println(e.getMessage());
		}
	}

	/*
	 * userBelongsWorkspace(UserID, WorkspaceID) Method designed to check if user
	 * belongs to a workspace.
	 * 
	 * Parameters: UserID: ID of user being checked. WorkspaceID: ID of the
	 * workspace being checked. Precondition: the connection to the db is active
	 * Table UserWorkspace is created.
	 *
	 * Postcondition: None Return values: Boolean, whether the user belongs to the
	 * workspace or not.
	 */
	private boolean userBelongsWorkspace(int UserID, int WorkspaceID)
	{
		try
		{
			String checkUserWorkspace = "SELECT COUNT(*) AS Total From UserWorkspace "
					+ "WHERE UserID = ? AND WorkspaceID = ?";

			PreparedStatement st = conn.prepareStatement(checkUserWorkspace);

			// ? = UserID being checked
			st.setInt(1, UserID);

			// ? = WorkspaceID being checked
			st.setInt(2, WorkspaceID);

			ResultSet r = st.executeQuery();

			if (r.next())
			{
				int total = r.getInt("Total");

				r.close();
				st.close();

				// user belongs if total > 0
				boolean result = total > 0;

				return result;
			}

			r.close();
			st.close();
		}

		catch (SQLException e)
		{
			System.err.println("Something went wrong checking the relationship UserWorkspace.");
			System.err.println(e.getMessage());
		}

		return false;
	}

	/*
	 * moveConversationToWorkspace() Method designed to move a conversation to a
	 * workspace.
	 * 
	 * Parameters: None.
	 * 
	 * Precondition: the connection to the db is active Workspace exists
	 * 
	 * Postcondition: conversation is updated if the user belongs to workspace and
	 * the conversation belongs to user. Return values: None
	 */
	private void moveConversationToWorkspace()
	{
		try
		{
			System.out.println("Enter UserID:");
			int userID = Integer.parseInt(scanner.nextLine().trim());

			System.out.println("Enter conversationID:");
			int conversationID = Integer.parseInt(scanner.nextLine().trim());

			System.out.println("Enter WorkspaceID:");
			int wsID = Integer.parseInt(scanner.nextLine().trim());

			if (!userBelongsWorkspace(userID, wsID))
			{
				System.out.println("User " + userID + " does not belong to this workspace.");
				return;
			}

			String moveConversation = "UPDATE Conversation " + "SET WorkspaceID = ? "
					+ "WHERE ConversationID = ? AND UserID = ?";

			PreparedStatement st = conn.prepareStatement(moveConversation);

			// ? = wsID
			st.setInt(1, wsID);

			// ? = conversation to move
			st.setInt(2, conversationID);

			// ? = userID
			st.setInt(3, userID);

			int rows = st.executeUpdate();

			st.close();

			if (rows > 0)
			{
				System.out.println("Success moving conversation to workspace");
			}

			else
			{
				System.out.println("conversation does not exist or does not belong to user " + userID + ".");
			}
		}
		catch (NumberFormatException e)
		{
			System.out.println("IDs must be integers.");

		}
		catch (SQLException e)
		{
			System.err.println("Something went wrong moving the conversation.");
			System.err.println(e.getMessage());
		}
	}

	/**
	 * ====================================================== Persona management --
	 * functionality 4
	 */

	/*
	 * getAvailablePersonaID() Method designed to get the next available PersonaID
	 * from the Oracle sequence. Precondition: the connection is active persona_seq
	 * is already in the db. Postcondition: it gets a new ID. Return values: the
	 * next available ID, or -1 if not possible to get one.
	 */
	private int getAvailablePersonaID()
	{
		// get next ID from sequence
		String sq = "SELECT persona_seq.NEXTVAL from dual";
		try
		{
			Statement s = conn.createStatement();
			ResultSet r = s.executeQuery(sq);

			// get the number
			if (r.next())
			{
				int nexID = r.getInt(1);
				r.close();
				s.close();
				return nexID;
			}

			r.close();
			s.close();
		}
		catch (SQLException e)
		{
			System.err.println("Error getting next available PersonaID.");
			System.err.println(e.getMessage());
		}

		return -1;
	}

	/*
	 * createPersona() Method designed to create a Persona. Precondition: the
	 * connection is active Persona_seq is already in the db.
	 * 
	 * Postcondition: new persona starts with versionID = 1 and deleteStatus = 0.
	 * 
	 * Return values: None
	 */
	private void createPersona()
	{
		try
		{
			int PersonaID = getAvailablePersonaID();

			// stop if we couldnot get valid ID
			if (PersonaID == -1)
			{
				System.out.println("Something went wrong creating Persona as personaID was not generated.");
				return;
			}

			// start with version 1
			int versionID = 1;

			System.out.println("Enter Persona name:");
			String name = scanner.nextLine().trim();

			System.out.println("Enter Persona guidelines:");
			String guidelines = scanner.nextLine().trim();

			String insertPersona = "INSERT INTO Persona(PersonaID, VersionID, PName, Guidelines, DateCreated, DeletedStatus) "
					+ "VALUES(?, ?, ?, ?, SYSDATE, 0)";

			PreparedStatement st = conn.prepareStatement(insertPersona);

			// ? = PersonaID
			st.setInt(1, PersonaID);

			// ? = VersionID
			st.setInt(2, versionID);

			// ? = name
			st.setString(3, name);

			// ? = guidelines
			st.setString(4, guidelines);

			st.executeUpdate();
			st.close();

			System.out.println("Persona created. PersonaID: " + PersonaID);
			System.out.println("VersionID: " + versionID);

		}
		catch (SQLException e)
		{
			System.err.println("Something went wrong creating Persona.");
			System.err.println(e.getMessage());
		}
	}

	/*
	 * activeConversations() Method designed to count the number of conversations
	 * using a Persona. Parameters: PersonaID: ID of persona being checked.
	 * versionID: ID of the version being checked. Precondition: the connection is
	 * active conversation table exists
	 * 
	 * Postcondition: None
	 * 
	 * Return values: Number of active conversations using the PersonaID and
	 * versionID or -1 if the count goes wrong.
	 */
	private int activeConversations(int PersonaID, int versionID)
	{
		try
		{
			String countActive = "SELECT COUNT(*) AS Total " + "FROM Conversation "
					+ "WHERE PersonaID = ? AND VersionID = ? AND IsActive = 1";

			PreparedStatement st = conn.prepareStatement(countActive);
			// ? = PersonaID
			st.setInt(1, PersonaID);

			// ? = VersionID
			st.setInt(2, versionID);

			ResultSet r = st.executeQuery();

			if (r.next())
			{
				int total = r.getInt("Total");

				r.close();
				st.close();

				return total;
			}

			r.close();
			st.close();

		}
		catch (SQLException e)
		{
			System.err.println("Something went wrong getting the number of active conversations.");
			System.err.println(e.getMessage());
		}
		// something went wrong if returned value is -1
		return -1;
	}

	/*
	 * deletePersona() Method designed to delete a Persona. Precondition: the
	 * connection is active Persona is already in the db.
	 * 
	 * Postcondition: No deletion if the persona version is used in more than 5
	 * conversations. DeletedStatus is set to 1. Return values: None
	 */
	private void deletePersona()
	{
		try
		{
			System.out.println("Enter PersonaID to delete:");
			int PersonaID = Integer.parseInt(scanner.nextLine().trim());

			System.out.println("Enter VersionID to delete:");
			int VersionID = Integer.parseInt(scanner.nextLine().trim());

			// get the number of active conversations based on input
			int activeConversations = activeConversations(PersonaID, VersionID);

			if (activeConversations == -1)
			{
				System.out.println("Could not get the number of active conversations. No persona was deleted.");
				return;
			}

			// persona cannot be deleted if it is in more than 5 active conversations
			if (activeConversations > 5)
			{
				System.out.println("Persona cannot be deleted, as it is used in more than 5 conversations.");
				return;
			}

			String deletePersona = "UPDATE Persona " + "SET DeletedStatus = 1 " + // don't actually delete from DB, so
																					// conversations can maintain
																					// context
					"WHERE PersonaID = ? and VersionID = ?";

			PreparedStatement st = conn.prepareStatement(deletePersona);

			// ? = PersonaID
			st.setInt(1, PersonaID);

			// ? = VersionID
			st.setInt(2, VersionID);

			int rows = st.executeUpdate();
			st.close();

			if (rows > 0)
			{
				System.out.println("Persona deleted.");
			}

			else
			{
				System.out.println("Persona was not found. No persona was deleted.");
			}
		}
		catch (NumberFormatException e)
		{
			System.out.println("IDs must be integers.");

		}
		catch (SQLException e)
		{
			System.err.println("Something went wrong deleting the Persona.");
			System.err.println(e.getMessage());
		}
	}

	/**
	 * ====================================================== Prompt library --
	 * functionality 5 - hasPromptEditPermission() - addPrompt() - updatePrompt()
	 */
	/**
	 * hasPromptEditPermissions() Determines whether the given user has sufficient
	 * role level within a workspace that allows them to edit Prompt Templates
	 * within that workspace
	 * 
	 * @param int userId -- the user's UserID value
	 * @param int workspaceId -- the workspace's WorkspaceID value
	 * 
	 * @return
	 * 
	 * @pre
	 * @post
	 */
	private boolean hasPromptEditPermission(int userId, int workspaceId) throws SQLException
	{
		String sql = "SELECT UWRole FROM UserWorkspace WHERE UserID = ? AND WorkspaceID = ?";
		try (PreparedStatement stmt = conn.prepareStatement(sql))
		{
			stmt.setInt(1, userId);
			stmt.setInt(2, workspaceId);
			try (ResultSet rs = stmt.executeQuery())
			{
				if (rs.next())
				{
					String role = rs.getString("UWRole");
					return "Admin".equals(role) || "Editor".equals(role);
				}
				return false;
			}
		}
	}

	/**
	 * addPrompt() Adds the specified prompt int the PromptTemplate table
	 * 
	 * @param int    workspaceId -- the workspace's WorkspaceID value
	 * @param int    userId -- the user's UserID value
	 * @param String category -- the PromptTemplate's Category value
	 * @param String visibility -- the PromptTemplate's Visiblity value
	 * @param String promptText -- the actual prompt given by this PromptTemplate
	 * 
	 * @return
	 * 
	 * @pre
	 * @post
	 */
	public int addPrompt(int workspaceId, int userId, String category, String visibility, String promptText)
			throws SQLException
	{
		if (!hasPromptEditPermission(userId, workspaceId))
		{
			throw new SecurityException("User does not have permission to add prompts in this workspace");
		}

		String insertPrompt = "INSERT INTO PromptTemplate (PromptID, Category, Visibility, Prompt, UserID) "
				+ "VALUES (prompt_seq.NEXTVAL, ?, ?, ?, ?)";
		int newPromptId;
		try (PreparedStatement stmt = conn.prepareStatement(insertPrompt, new String[]
		{ "PromptID" }))
		{
			stmt.setString(1, category);
			stmt.setString(2, visibility);
			stmt.setString(3, promptText);
			stmt.setInt(4, userId);
			stmt.executeUpdate();
			try (ResultSet rs = stmt.getGeneratedKeys())
			{
				if (rs.next())
					newPromptId = rs.getInt(1);
				else
					throw new SQLException("Failed to get generated PromptID");
			}
		}

		String linkToWorkspace = "INSERT INTO SpaceTemplates (WorkspaceID, PromptID) VALUES (?, ?)";
		try (PreparedStatement stmt = conn.prepareStatement(linkToWorkspace))
		{
			stmt.setInt(1, workspaceId);
			stmt.setInt(2, newPromptId);
			stmt.executeUpdate();
		}
		return newPromptId;
	}

	/**
	 * updatePrompt() Updates the specified existing prompt int the PromptTemplate
	 * table
	 * 
	 * @param int    promptId -- the PromptTemplate's PromptID value
	 * @param int    userId -- the user's UserID value
	 * @param String newCategory -- the new value for the PromptTemplate's Category
	 *               attribute
	 * @param String visibility -- the new value for the PromptTemplate's Visiblity
	 *               attribute
	 * @param String promptText -- the new value for the PromptTemplate's Prompt
	 *               attribute
	 * 
	 * @return
	 * 
	 * @pre
	 * @post
	 */
	public boolean updatePrompt(
			int promptId, int userId, String newCategory, String newVisibility, String newPromptText
	) throws SQLException
	{
		// find which workspace owns this prompt
		String findWorkspace = "SELECT WorkspaceID FROM SpaceTemplates WHERE PromptID = ?";
		int workspaceId;
		try (PreparedStatement stmt = conn.prepareStatement(findWorkspace))
		{
			stmt.setInt(1, promptId);
			try (ResultSet rs = stmt.executeQuery())
			{
				if (!rs.next())
					throw new SQLException("Prompt not found or not linked to any workspace");
				workspaceId = rs.getInt("WorkspaceID");
			}
		}

		if (!hasPromptEditPermission(userId, workspaceId))
		{
			throw new SecurityException("User does not have permission to edit this prompt");
		}

		String updateSql = "UPDATE PromptTemplate SET Category = ?, Visibility = ?, Prompt = ? WHERE PromptID = ?";
		try (PreparedStatement stmt = conn.prepareStatement(updateSql))
		{
			stmt.setString(1, newCategory);
			stmt.setString(2, newVisibility);
			stmt.setString(3, newPromptText);
			stmt.setInt(4, promptId);
			return stmt.executeUpdate() > 0;
		}
	}

	/**
	 * ====================================================== Subscription tracking
	 * -- functionality 6 - updateUserSubscription() - getUserDailyLimit() -
	 * countUserMessagesToday() - insertMessageIfWithinLimit()
	 */
	/**
	 * updateUserSubscription() Updates a user's subscription status -- which
	 * subscription tier their account is associated with
	 * 
	 * @param int userId -- the user's UserID value
	 * @param int newTierId -- the TierID value of the subscription tier that this
	 *            user is being assigned
	 * 
	 * @return
	 * 
	 * @pre
	 * @post
	 */
	public boolean updateUserSubscription(int userId, int newTierId) throws SQLException
	{
		String sql = "UPDATE Users SET TierID = ? WHERE UserID = ?";
		try (PreparedStatement stmt = conn.prepareStatement(sql))
		{
			stmt.setInt(1, newTierId);
			stmt.setInt(2, userId);
			return stmt.executeUpdate() > 0;
		}
	}

	/**
	 * getUserDailyLimit() Gets the given user's limit on max messages per day based
	 * on their subscription tier
	 * 
	 * @param int userId -- the user's UserID value
	 * 
	 * @return
	 * 
	 * @pre
	 * @post
	 */
	private int getUserDailyLimit(int userId) throws SQLException
	{
		String sql = "SELECT m.MaxMsgPerDay FROM Users u " + "JOIN MembershipTier m ON u.TierID = m.TierID "
				+ "WHERE u.UserID = ?";
		try (PreparedStatement stmt = conn.prepareStatement(sql))
		{
			stmt.setInt(1, userId);
			try (ResultSet rs = stmt.executeQuery())
			{
				if (rs.next())
					return rs.getInt("MaxMsgPerDay");
				else
					throw new SQLException("User does not have a subscription tier assigned");
			}
		}
	}

	/**
	 * countUserMessagesToday() Calculates the given user's number of messages sent
	 * today
	 * 
	 * @param int userId -- the user's UserID value
	 * 
	 * @return
	 * 
	 * @pre
	 * @post
	 */
	private int countUserMessagesToday(int userId) throws SQLException
	{
		String sql = "SELECT COUNT(*) FROM Message m " + "JOIN Conversation c ON m.ConversationID = c.ConversationID "
				+ "WHERE c.UserID = ? AND TRUNC(m.MTime) = TRUNC(SYSDATE)"; // TRUNC removes time, just keeps day/year
		try (PreparedStatement stmt = conn.prepareStatement(sql))
		{
			stmt.setInt(1, userId);
			try (ResultSet rs = stmt.executeQuery())
			{
				if (rs.next())
					return rs.getInt(1);
				return 0;
			}
		}
	}

	/**
	 * insertMessageIfWithinLimit() Checks the sender to see if they have reached
	 * their message limit for the day, if not, then add the message to the DB
	 * 
	 * @param int    userId -- the user's UserID value
	 * @param int    conversationId -- the containing Conversation's ConversationID
	 *               value
	 * @param String senderRole -- the Role value of the sender (User) in the
	 *               UserWorkspace relationship
	 * @param String promptText -- the new value for the PromptTemplate's Prompt
	 *               attribute
	 * 
	 * @return
	 * 
	 * @pre
	 * @post
	 */
	public int insertMessageIfWithinLimit(int userId, int conversationId, String senderRole, String content)
			throws SQLException
	{
		conn.setAutoCommit(false);
		try
		{
			int todayCount = countUserMessagesToday(userId);
			int limit = getUserDailyLimit(userId);
			if (todayCount >= limit)
			{
				conn.rollback();
				return -1; // limit exceeded
			}

			String verifySql = "SELECT 1 FROM Conversation WHERE ConversationID = ? AND UserID = ?";
			try (PreparedStatement ps = conn.prepareStatement(verifySql))
			{
				ps.setInt(1, conversationId);
				ps.setInt(2, userId);
				try (ResultSet rs = ps.executeQuery())
				{
					if (!rs.next())
					{
						throw new SQLException("Conversation does not belong to this user");
					}
				}
			}

			String insertMsg = "INSERT INTO Message (MessageID, SenderRole, MTime, Content, ConversationID) "
					+ "VALUES (msg_seq.NEXTVAL, ?, SYSDATE, ?, ?)";
			int newMessageId;
			try (PreparedStatement stmt = conn.prepareStatement(insertMsg, new String[]
			{ "MessageID" }))
			{
				stmt.setString(1, senderRole);
				stmt.setString(2, content);
				stmt.setInt(3, conversationId);
				stmt.executeUpdate();
				try (ResultSet rs = stmt.getGeneratedKeys())
				{
					if (rs.next())
						newMessageId = rs.getInt(1);
					else
						throw new SQLException("Failed to get MessageID");
				}
			}

			conn.commit();
			return newMessageId;
		}
		catch (SQLException e)
		{
			conn.rollback();
			throw e;
		}
		finally
		{
			conn.setAutoCommit(true);
		}
	}

	/**
	 * ====================================================== Billing operations --
	 * functionality 7 - generateInvoice() - markInvoicePaid()
	 */

	/*
	 * generateInvoice()
	 *
	 * Purpose: Reads a UserID from the user, looks up that user's monthly tier fee,
	 * and inserts a new unpaid invoice into the Invoice table.
	 *
	 * Parameters: none
	 *
	 * Return value: none
	 *
	 * Pre-condition: A valid UserID and the user has a membership tier assigned
	 * Post-condition: A new row is added to Invoice with Status = 'unpaid' and
	 * Amount set to the user's current tier fee.
	 */
	private void generateInvoice() throws SQLException
	{
		System.out.print("Enter UserID: ");
		int userId = Integer.parseInt(scanner.nextLine().trim()); // ID of the user being billed

		// join Users to MembershipTier to get the monthly fee for this user's tier
		String feeSql = "SELECT mt.Fee FROM Users u " + "JOIN MembershipTier mt ON u.TierID = mt.TierID "
				+ "WHERE u.UserID = ?";
		double fee; // monthly fee amount pulled from MembershipTier
		try (PreparedStatement stmt = conn.prepareStatement(feeSql))
		{
			stmt.setInt(1, userId);
			try (ResultSet rs = stmt.executeQuery())
			{
				if (!rs.next())
					throw new SQLException("User not found or has no membership tier.");
				fee = rs.getDouble("Fee");
			}
		}

		// "Date" is quoted because DATE is a reserved word in Oracle SQL
		String insertSql = "INSERT INTO Invoice (InvoiceID, UserID, Amount, IDate, IStatus) "
				+ "VALUES (invoice_seq.NEXTVAL, ?, ?, SYSDATE, 'unpaid')";
		try (PreparedStatement stmt = conn.prepareStatement(insertSql, new String[]
		{ "InvoiceID" }))
		{
			stmt.setInt(1, userId);
			stmt.setDouble(2, fee);
			stmt.executeUpdate();
			try (ResultSet rs = stmt.getGeneratedKeys())
			{
				if (rs.next())
					System.out.printf("Invoice generated (ID: %d) for $%.2f -- status: unpaid%n", rs.getInt(1), fee);
			}
		}
	}

	/*
	 * markInvoicePaid()
	 *
	 * Purpose: Reads an InvoiceID from the user and sets that invoice's Status to
	 * 'paid'. Only updates if its currently 'unpaid'.
	 *
	 * Parameters: none
	 *
	 * Return value: none
	 *
	 * Pre-condition: A valid InvoiceID is entered and the Invoice table exists.
	 * Post-condition: The matching invoice's Status is set to 'paid', or the user
	 * is told the invoice was not found.
	 */
	private void markInvoicePaid() throws SQLException
	{
		System.out.print("Enter InvoiceID: ");
		int invoiceId = Integer.parseInt(scanner.nextLine().trim()); // ID of the invoice to mark paid

		// guard on Status = 'unpaid' so already-paid invoices are not touched
		String sql = "UPDATE Invoice SET IStatus = 'paid' WHERE InvoiceID = ? AND IStatus = 'unpaid'";
		try (PreparedStatement stmt = conn.prepareStatement(sql))
		{
			stmt.setInt(1, invoiceId);
			int rows = stmt.executeUpdate();
			if (rows > 0)
				System.out.println("Invoice marked as paid.");
			else
				System.out.println("Invoice not found or is already paid.");
		}
	}

	/**
	 * ====================================================== Support Ticket
	 * Lifecycle -- functionality 8 - createSupportTicket() - assignTicketToAgent()
	 * - updateTicketResolution()
	 */

	/*
	 * createSupportTicket()
	 *
	 * Purpose: Lists available support agents, then reads a UserID, topic, and
	 * AgentID from the user and inserts a new open ticket into the SupportTicket
	 * table.
	 *
	 * Parameters: none Return value: none
	 *
	 * Pre-condition: Valid UserID and AgentID are entered, both the Users and
	 * SupportAgent tables have matching rows. Post-condition: A new row is added to
	 * SupportTicket with DateOpened = today and DateClosed.
	 */
	private void createSupportTicket() throws SQLException
	{
		// show every agent so the user can pick a valid AgentID
		String agentSql = "SELECT AgentID, SName FROM SupportAgent ORDER BY AgentID";
		try (PreparedStatement stmt = conn.prepareStatement(agentSql); ResultSet rs = stmt.executeQuery())
		{
			System.out.println("\nAvailable agents:");
			while (rs.next())
				System.out.printf("  %d - %s%n", rs.getInt("AgentID"), rs.getString("SName"));
		}

		System.out.print("Enter UserID: ");
		int userId = Integer.parseInt(scanner.nextLine().trim()); // user opening the ticket
		System.out.print("Enter Topic: ");
		String topic = scanner.nextLine().trim(); // short description
		System.out.print("Enter AgentID: ");
		int agentId = Integer.parseInt(scanner.nextLine().trim()); // agent handling the ticket

		String insertSql = "INSERT INTO SupportTicket (TicketID, UserID, AgentID, Topic, DateOpened) "
				+ "VALUES (ticket_seq.NEXTVAL, ?, ?, ?, SYSDATE)";
		try (PreparedStatement stmt = conn.prepareStatement(insertSql, new String[]
		{ "TicketID" }))
		{
			stmt.setInt(1, userId);
			stmt.setInt(2, agentId);
			stmt.setString(3, topic);
			stmt.executeUpdate();
			try (ResultSet rs = stmt.getGeneratedKeys())
			{
				if (rs.next())
					System.out.printf("Support ticket created (ID: %d).%n", rs.getInt(1));
			}
		}
	}

	/*
	 * assignTicketToAgent()
	 *
	 * Purpose: Reads a TicketID and a new AgentID from the user and reassigns the
	 * ticket to the chosen agent.
	 *
	 * Parameters: none
	 *
	 * Return value: none
	 *
	 * Pre-condition: A valid TicketID for an open ticket and a valid AgentID are
	 * entered. Post-condition: The ticket's AgentID is updated, or the user is told
	 * the ticket was not found or is already closed.
	 */
	private void assignTicketToAgent() throws SQLException
	{
		System.out.print("Enter TicketID: ");
		int ticketId = Integer.parseInt(scanner.nextLine().trim()); // ticket to reassign

		// show every agent so the user can pick a valid AgentID
		String agentSql = "SELECT AgentID, SName FROM SupportAgent ORDER BY AgentID";
		try (PreparedStatement stmt = conn.prepareStatement(agentSql); ResultSet rs = stmt.executeQuery())
		{
			System.out.println("\nAvailable agents:");
			while (rs.next())
				System.out.printf("  %d - %s%n", rs.getInt("AgentID"), rs.getString("SName"));
		}

		System.out.print("Enter new AgentID: ");
		int agentId = Integer.parseInt(scanner.nextLine().trim()); // agent to assign the ticket to

		// DateClosed IS NULL ensures we only touch open tickets
		String sql = "UPDATE SupportTicket SET AgentID = ? WHERE TicketID = ? AND DateClosed IS NULL";
		try (PreparedStatement stmt = conn.prepareStatement(sql))
		{
			stmt.setInt(1, agentId);
			stmt.setInt(2, ticketId);
			int rows = stmt.executeUpdate();
			if (rows > 0)
				System.out.println("Ticket assigned to agent.");
			else
				System.out.println("Ticket not found or is already closed.");
		}
	}

	/*
	 * updateTicketResolution()
	 *
	 * Purpose: Reads a TicketID and resolution status from the user, closes the
	 * ticket by setting DateClosed to today and Status to the given value.
	 *
	 * Parameters: none
	 *
	 * Return value: none
	 *
	 * Pre-condition: A valid TicketID for an open ticket is entered and status is
	 * either 'Resolved' or 'Escalated'. Post-condition: DateClosed and Status are
	 * set on the ticket.
	 */
	private void updateTicketResolution() throws SQLException
	{
		System.out.print("Enter TicketID: ");
		int ticketId = Integer.parseInt(scanner.nextLine().trim()); // ticket being closed
		System.out.print("Enter resolution status (Resolved/Escalated): ");
		String status = scanner.nextLine().trim(); // final outcome of the ticket

		// validate before entering database
		if (!status.equals("Resolved") && !status.equals("Escalated"))
		{
			System.out.println("Status must be 'Resolved' or 'Escalated'.");
			return;
		}

		// DateClosed IS NULL guards against closing an already-closed ticket
		String updateSql = "UPDATE SupportTicket SET STStatus = ?, DateClosed = SYSDATE "
				+ "WHERE TicketID = ? AND DateClosed IS NULL";
		try (PreparedStatement stmt = conn.prepareStatement(updateSql))
		{
			stmt.setString(1, status);
			stmt.setInt(2, ticketId);
			int rows = stmt.executeUpdate();
			if (rows == 0)
			{
				System.out.println("Ticket not found or is already closed.");
				return;
			}
		}

		// compute duration as the difference in days between open and close dates
		String durSql = "SELECT TRUNC(DateClosed) - TRUNC(DateOpened) AS DurationDays "
				+ "FROM SupportTicket WHERE TicketID = ?";
		try (PreparedStatement stmt = conn.prepareStatement(durSql))
		{
			stmt.setInt(1, ticketId);
			try (ResultSet rs = stmt.executeQuery())
			{
				if (rs.next())
					System.out.printf("Ticket closed as '%s'. Resolution duration: %d day(s).%n", status,
							rs.getInt("DurationDays"));
			}
		}
	}

	/*
	 * Required queries - queryBookmarkedMessages() - queryUnpaidInvoices() -
	 * queryMostHelpfulPersona() - queryWorkspaceMembers()
	 */
	/**
	 * queryBookmarkedMessages() For a given User, list all their Bookmarked
	 * messages across all conversations, including the conversation title and the
	 * timestamp.
	 * 
	 * @return N/A
	 * 
	 * @pre
	 * @post
	 */
	private void queryBookmarkedMessages() throws SQLException
	{
		System.out.print("Enter UserID: ");
		int userId = Integer.parseInt(scanner.nextLine().trim());

		String sql = """
			SELECT c.Title, m.MTime, m.Content
			FROM UserBookmarks ub
			JOIN Message m ON ub.MessageID = m.MessageID
			JOIN Conversation c ON m.ConversationID = c.ConversationID
			WHERE ub.UserID = ?
			ORDER BY m.MTime DESC
		""";
		
		try (PreparedStatement stmt = conn.prepareStatement(sql))
		{
			stmt.setInt(1, userId);
			try (ResultSet rs = stmt.executeQuery())
			{
				System.out.println("\nBookmarked Messages:");
				System.out.println("--------------------------------------------------");
				boolean found = false;
				while (rs.next())
				{
					found = true;
					System.out.printf("Conversation: %s\nTimestamp: %s\nMessage: %s\n\n", rs.getString("Title"),
							rs.getTimestamp("MTime"), rs.getString("Content"));
				}
				if (!found)
					System.out.println("No bookmarked messages for this user.");
			}
		}
	}

	/**
	 * queryUnpaidInvoices() List all users who have “Unpaid” invoices, including
	 * their email, the total amount owed, and the date of their last conversation.
	 * 
	 * @return N/A
	 * 
	 * @pre
	 * @post
	 */
	private void queryUnpaidInvoices() throws SQLException
	{
		String sql = """
			SELECT u.Email, SUM(i.Amount) AS TotalOwed, MAX(c.DateCreated) AS LastConversationDate
			FROM Users u
			JOIN Invoice i ON u.UserID = i.UserID
			LEFT JOIN Conversation c ON u.UserID = c.UserID
			WHERE i.IStatus = 'unpaid'
			GROUP BY u.Email
			ORDER BY TotalOwed DESC
		""";
		
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql))
		{
			System.out.println("\nUsers with Unpaid Invoices:");
			System.out.printf("%-30s %15s %20s\n", "Email", "Total Owed", "Last Conversation Date");
			System.out.println("--------------------------------------------------------------------");
			boolean found = false;
			while (rs.next())
			{
				found = true;
				System.out.printf("%-30s $%14.2f %20s\n", rs.getString("Email"), rs.getDouble("TotalOwed"),
						rs.getDate("LastConversationDate") == null ? "Never" : rs.getDate("LastConversationDate"));
			}
			if (!found)
				System.out.println("No unpaid invoices.");
		}
	}

	/**
	 * queryMostHelpfulPersona() Identify the “Most Helpful” Persona: List the
	 * persona name that has received the highest percentage of “Thumbs Up” feedback
	 * across all conversations linked to it.
   *
	 * @note   isThumbsUp is just 1 if thumbs up and 0 if not
	 * @return N/A
	 * 
	 * @pre
	 * @post
	 */
	private void queryMostHelpfulPersona() throws SQLException
	{
		String sql = """
					SELECT p.PName,
				     			COUNT(f.FeedbackID) AS TotalFeedback,
				     			SUM(f.IsThumbsUp) AS ThumbsUpCount,
				     			ROUND(100.0 * SUM(f.IsThumbsUp) / COUNT(f.FeedbackID), 2) AS Percentage
					FROM Persona p
					JOIN Conversation c ON p.PersonaID = c.PersonaID AND p.VersionID = c.VersionID
					JOIN Message m ON c.ConversationID = m.ConversationID
					JOIN Feedback f ON m.MessageID = f.MessageID
					WHERE p.DeletedStatus = 0
					GROUP BY p.PName
					HAVING COUNT(f.FeedbackID) > 0
					ORDER BY Percentage DESC
					FETCH FIRST 1 ROW ONLY;
				""";
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql))
		{
			System.out.println("\nMost Helpful Persona:");
			if (rs.next())
			{
				System.out.printf("Name: %s\nThumbs Up Percentage: %.2f%% (based on %d ratings)\n",
						rs.getString("PName"), rs.getDouble("Percentage"), rs.getInt("TotalFeedback"));
			}
			else
			{
				System.out.println("No feedback data available.");
			}
		}
	}

	/**
	 * queryWorkspaceMembers() Additional non-trivial query of our own design --
	 * List workspaces where a given user has Admin role, along with the number of
	 * members in that workspace.
	 * 
	 * @return N/A
	 * 
	 * @pre
	 * @post
	 */
	private void queryWorkspaceMembers() throws SQLException
	{
		System.out.print("Enter UserID to see workspaces where they are Admin: ");
		int userId = Integer.parseInt(scanner.nextLine().trim());

		String sql = """
			SELECT w.WName AS WorkspaceName, COUNT(uw2.UserID) AS MemberCount
			FROM Workspace w
			JOIN UserWorkspace uwAdmin ON w.WorkspaceID = uwAdmin.WorkspaceID
			LEFT JOIN UserWorkspace uw2 ON w.WorkspaceID = uw2.WorkspaceID
			WHERE uwAdmin.UserID = ? AND uwAdmin.UWRole = 'Admin'
			GROUP BY w.WName
			ORDER BY w.WName
		""";

		try (PreparedStatement stmt = conn.prepareStatement(sql))
		{
			stmt.setInt(1, userId);
			try (ResultSet rs = stmt.executeQuery())
			{
				System.out.printf("\nWorkspaces where User %d is Admin:\n", userId);
				System.out.printf("%-30s %15s\n", "Workspace Name", "Member Count");
				System.out.println("---------------------------------------------");
				boolean found = false;
				while (rs.next())
				{
					found = true;
					System.out.printf("%-30s %15d\n", rs.getString("WorkspaceName"), rs.getInt("MemberCount"));
				}
				if (!found)
					System.out.println("User is not an Admin of any workspace.");
			}
		}
	}

	/**
	 * Current UI
	 */
	public void runMainMenu()
	{
		while (true)
		{
			System.out.println("\n" + "=".repeat(60));
			System.out.println("LLM USER-FACING ECOSYSTEM MANAGEMENT SYSTEM");
			System.out.println("=".repeat(60));
			System.out.println("--- FUNCTIONALITIES ---");
			System.out.println("1.  Manage User Accounts (add/update/delete)");
			System.out.println("2.  Conversations & Messages");
			System.out.println("3.  Workspace Organization");
			System.out.println("4.  Persona Management");
			System.out.println("5.  Prompt Library (add/update)");
			System.out.println("6.  Subscription Tracking (update tier, message limit)");
			System.out.println("7.  Billing Operations");
			System.out.println("8.  Support Ticket Lifecycle");
			System.out.println("\n--- REQUIRED QUERIES ---");
			System.out.println("9.  Query 1: Bookmarked messages for a user");
			System.out.println("10. Query 2: Users with unpaid invoices");
			System.out.println("11. Query 3: Most helpful persona");
			System.out.println("12. Query 4: Custom query (workspace members for admin)");
			System.out.println("0.  Exit");
			System.out.print("Choice: ");

			String choice = scanner.nextLine().trim();
			try
			{
				switch (choice)
				{
				case "1":
					userAccountSubMenu();
					break;
				case "2":
					conversationSubMenu();
					break;
				case "3":
					workspaceSubMenu();
					break;
				case "4":
					personaSubMenu();
					break;
				case "5":
					promptLibrarySubMenu();
					break;
				case "6":
					subscriptionSubMenu();
					break;
				case "7":
					billingSubMenu();
					break;
				case "8":
					supportSubMenu();
					break;
				case "9":
					queryBookmarkedMessages();
					break;
				case "10":
					queryUnpaidInvoices();
					break;
				case "11":
					queryMostHelpfulPersona();
					break;
				case "12":
					queryWorkspaceMembers();
					break;
				case "0":
					System.out.println("Exiting...");
					conn.close();
					return;
				default:
					System.out.println("Invalid choice. Please enter 0-12.");
				}
			}
			catch (Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				try
				{
					conn.rollback();
				}
				catch (SQLException ex)
				{
					// ignore
				}
			}
			System.out.println("\nPress Enter to continue...");
			scanner.nextLine();
		}
	}

	// sub-menus
	private void userAccountSubMenu()
	{
		System.out.println("\n--- User Account Management ---");
		System.out.println("1. Add User");
		System.out.println("2. Update User");
		System.out.println("3. Delete User");
		System.out.print("Choice: ");
		String sub = scanner.nextLine().trim();
		try
		{
			switch (sub)
			{
			case "1":
				System.out.print("Name: ");
				String name = scanner.nextLine();
				System.out.print("Email: ");
				String email = scanner.nextLine();
				System.out.print("Preferred UI: ");
				String ui = scanner.nextLine();
				addUser(name, email, ui);
				System.out.println("User added successfully.");
				break;
			case "2":
				System.out.print("UserID to update: ");
				int userId = Integer.parseInt(scanner.nextLine());
				System.out.print("New Name: ");
				String newName = scanner.nextLine();
				System.out.print("New Email: ");
				String newEmail = scanner.nextLine();
				System.out.print("New Preferred UI Language: ");
				String newUI = scanner.nextLine();
				int newTierId = -1;
				while (newTierId < 1 || newTierId > 3)
				{
					System.out.print("New TierID (must be 1,2,3...): ");
					String input = scanner.nextLine();
					if (input.isEmpty())
						continue;
					newTierId = Integer.parseInt(input);
				}
				updateUser(userId, newName, newEmail, newUI, newTierId);
				System.out.println("User updated successfully.");
				break;
			case "3":
				System.out.print("UserID to delete: ");
				int delUserId = Integer.parseInt(scanner.nextLine());
				deleteUser(delUserId);
				break;
			default:
				System.out.println("Invalid.");
			}
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
		}
	}

	private void conversationSubMenu()
	{
		System.out.println("\n--- Conversations & Messages ---");
		System.out.println("1. Start New Conversation");
		System.out.println("2. Update Message Feedback");
		System.out.println("3. Bookmark a Message");
		System.out.println("4. Add Message to Conversation (with limit check)");
		System.out.print("Choice: ");
		String sub = scanner.nextLine().trim();
		try
		{
			switch (sub)
			{
			case "1":
				startConversation();
				break;
			case "2":
				updateMessageFeedback();
				break;
			case "3":
				bookmarkMessage();
				break;
			case "4":
				addMessageToConversation();
				break; // new wrapper
			default:
				System.out.println("Invalid.");
			}
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
		}
	}

	private void workspaceSubMenu()
	{
		System.out.println("\n--- Workspace Organization ---");
		System.out.println("1. Create Workspace");
		System.out.println("2. Modify Workspace");
		System.out.println("3. Move Conversation to Workspace");
		System.out.print("Choice: ");
		String sub = scanner.nextLine().trim();
		try
		{
			switch (sub)
			{
			case "1":
				createWorkspace();
				break;
			case "2":
				modifyWorkspace();
				break;
			case "3":
				moveConversationToWorkspace();
				break;
			default:
				System.out.println("Invalid.");
			}
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
		}
	}

	private void personaSubMenu()
	{
		System.out.println("\n--- Persona Management ---");
		System.out.println("1. Create Persona");
		System.out.println("2. Delete Persona");
		System.out.print("Choice: ");
		String sub = scanner.nextLine().trim();
		try
		{
			switch (sub)
			{
			case "1":
				createPersona();
				break;
			case "2":
				deletePersona();
				break;
			default:
				System.out.println("Invalid.");
			}
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
		}
	}

	private void promptLibrarySubMenu()
	{
		System.out.println("\n--- Prompt Library ---");
		System.out.println("1. Add Prompt Template");
		System.out.println("2. Update Prompt Template");
		System.out.print("Choice: ");
		String sub = scanner.nextLine().trim();
		try
		{
			if (sub.equals("1"))
			{
				System.out.print("WorkspaceID: ");
				int ws = Integer.parseInt(scanner.nextLine());
				System.out.print("UserID: ");
				int uid = Integer.parseInt(scanner.nextLine());
				System.out.print("Category: ");
				String cat = scanner.nextLine();
				System.out.print("Visibility (private/shared): ");
				String vis = scanner.nextLine();
				if (!vis.equals("private") && !vis.equals("shared"))
				{
					System.out.println("Visibility must be 'private' or 'shared'.");
					return;
				}
				System.out.print("Prompt text: ");
				String txt = scanner.nextLine();
				int newId = addPrompt(ws, uid, cat, vis, txt);
				System.out.println("Prompt added with ID: " + newId);
			}
			else if (sub.equals("2"))
			{
				System.out.print("PromptID: ");
				int pid = Integer.parseInt(scanner.nextLine());
				System.out.print("UserID: ");
				int uid = Integer.parseInt(scanner.nextLine());
				System.out.print("New Category: ");
				String cat = scanner.nextLine();
				System.out.print("New Visibility (private/shared): ");
				String vis = scanner.nextLine();
				if (!vis.equals("private") && !vis.equals("shared"))
				{
					System.out.println("Visibility must be 'private' or 'shared'.");
					return;
				}
				System.out.print("New Prompt text: ");
				String txt = scanner.nextLine();
				boolean ok = updatePrompt(pid, uid, cat, vis, txt);
				System.out.println(ok ? "Updated" : "Update failed");
			}
			else
			{
				System.out.println("Invalid choice.");
			}
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
		}
	}

	private void subscriptionSubMenu()
	{
		System.out.println("\n--- Subscription Tracking ---");
		System.out.println("1. Update User Subscription Tier");
		System.out.print("Choice: ");
		String sub = scanner.nextLine().trim();
		try
		{
			if (sub.equals("1"))
			{
				System.out.print("UserID: ");
				int uid = Integer.parseInt(scanner.nextLine());
				System.out.print("New TierID: ");
				int tid = Integer.parseInt(scanner.nextLine());
				boolean ok = updateUserSubscription(uid, tid);
				System.out.println(ok ? "Subscription updated" : "Update failed");
			}
			else
			{
				System.out.println("Invalid choice.");
			}
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
		}
	}

	private void billingSubMenu()
	{
		System.out.println("\n--- Billing Operations ---");
		System.out.println("1. Generate Invoice");
		System.out.println("2. Mark Invoice as Paid");
		System.out.print("Choice: ");
		String sub = scanner.nextLine().trim();
		try
		{
			switch (sub)
			{
			case "1":
				generateInvoice();
				break;
			case "2":
				markInvoicePaid();
				break;
			default:
				System.out.println("Invalid.");
			}
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
		}
	}

	private void supportSubMenu()
	{
		System.out.println("\n--- Support Ticket Lifecycle ---");
		System.out.println("1. Create Ticket");
		System.out.println("2. Assign Ticket to Agent");
		System.out.println("3. Update Ticket Resolution Status");
		System.out.print("Choice: ");
		String sub = scanner.nextLine().trim();
		try
		{
			switch (sub)
			{
			case "1":
				createSupportTicket();
				break;
			case "2":
				assignTicketToAgent();
				break;
			case "3":
				updateTicketResolution();
				break;
			default:
				System.out.println("Invalid.");
			}
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
		}
	}

	public static void main(String[] args)
	{
		LLMEcosystem app = new LLMEcosystem();
		app.runMainMenu();
	}
}
