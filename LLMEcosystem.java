/**
 * 	Authors: 
 * 			Luis Miranda			mirandaquijada@arizona.edu
 * 			Shale Van Cleve			shalevancleve@arizona.edu
 * 			Zoltan Kotha			zkotha@arizona.edu
 * 			Ian Sanchez Lopez		iansanchezl@arizona.edu
 * 	Course: 
 * 			CSC460
 * 	Assignment: 
 * 			Program #4: Database Design and Implementation
 * 	Instructors/TAs: 
 * 			Lester I. McCann, Jianwei Shen, Muhammad Bilal
 * 	Due Date: 
 * 			5/5/2026 2:00 PM
 * 	Problem Desc: 
 * 			Database-driven information management system for an LLM User-Facing Ecosystem
 * 	Additional Solution info:
 * 			N/A
 * 	Operational Requirements:
 *          Java 8+, input file must be readable mbox format
 *          Usage: TODO
 *          Output: TODO
 * 	Data Structures: 
 * 			N/A
 * 	File Reading: 
 * 			N/A
 */

import java.sql.*;
import java.util.Scanner;

public class LLMEcosystem
{

	private static final String DB_URL = "jdbc:oracle:thin:@aloe.cs.arizona.edu:1521:oracle";
	private static final String DB_USER = "shalevancleve";
	private static final String DB_PASS = "a2532";

	private Connection conn;

	public LLMEcosystem()
	{
		try
		{
			Class.forName("oracle.jdbc.OracleDriver");
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
			System.out.println("Connected to Oracle DB");
		}
		catch (Exception e)
		{
			System.err.println("Database connection error: " + e.getMessage());
			System.exit(1);
		}
	}
	
	/**======================================================
	 * Manage user accounts -- functionality 1
	 * addUser()
	 * updateUser()
	 * deleteUser()
	 */
	
	/*
	* Adds a new user to the User table
	*
	* @param  name, the name of the new user to be added
	* @param  email, the email address of a user to be added
	* @param  preferredUI, the UI language of the LLM
	* @return None
	* @note   when creating user. tier defaults to "Free" with tierID 1
	*/
	public static void addUser(String name, String email, String preferredUI) {
		String insertStmt = "INSERT INTO User (Name, Email, preferredUI, DateCreated, TierID) "
							+ "VALUES (user_seq.NEXTVAL, ?,  ?, ?, SYSDATE, 1)";
		try {
			PreparedStatement stmt = conn.prepareStatement(insertStmt);
			stmt.setString(1, name);
			stmt.setString(2, email);
			stmt.setString(3, preferredUI);
			stmt.executeUpdate();
		}
		catch (SQLException e) {
			System.err.println("Error adding user to database");
			return;
		}
	}

	/*
	* Updates a user's info in the database.
	*
	* @param  UserID, the unique id of the user being updated
	* @param  newName, the (maybe) updated name of a user to insert
	* @param  newEmail, the (maybe) new email address of a user to insert
	* @param  preferredUI, the (maybe) new preferred language of the AI for a user
	* @param  tierID, the (maybe) new model tier a user wants to subscribe to
	* @return None
	* @note   dateCreated & UserID fields remain unchanged
	*/
	public static void updateUser(int UserID, String newName, String newEmail, String preferredUI, int tierID) throws SQLException {
		String updateQuery = "UPDATE Users SET Name = ?, Email = ?, preferredUI = ?, tierID = ? WHERE UserID = ?";
		PreparedStatement stmt = conn.prepareStatement(updateQuery);
		stmt.setString(1,newName); 
		stmt.setString(2, newEmail);
		stmt.setString(3, preferredUI);
		stmt.setInt(4, tierID);
		stmt.setInt(5, UserID);
		stmt.executeUpdate()
	}


	// checks if a user has unpaid invoices or an unclosed support ticket
	public static boolean checkUnpaidInvoicesOrSupportTickets(int UserID) throws SQLException {
		String query = "SELECT DISTINCT UserID FROM Invoice i, SupportTicket t WHERE t.DateClosed IS NULL OR i.status = 'unpaid'";
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		if(rs != null) {
			while(rs.next()) {
				if(rs.getInt() == UserID)
					return true;
			}
		}
		return false;
	}
	
	/*
	* Deletes a user in the database
	*
	*
	* @note deletion fails if invoices unpaid or open support tickets
	*/
	public static void deleteUser(int UserID) throws SQLException {
		boolean failed = checkUnpaidInvoicesOrSupportTickets(int UserID);
		if(failed) {
			System.out.println("Cannot delete user account: invoice is unpaid
								+ or a support ticket is open");
			return;
		}
		String deleteStmt = "DELETE FROM Users WHERE UserID = ?";
		PreparedStatement stmt = conn.prepareStatement(deleteStmt);
		stmt.setInt(1, UserID);
		stmt.executeUpdate();
	}
	
	
	/**======================================================
	 * Handle Conversations & messages -- functionality 2
	 * startConversation()
	 * addMessageToConversation()
	 * updateMessageFeedback()
	 * deleteConversationsAndMessages
	 */
	//TODO
	
	
	
	/**======================================================
	 * workspace organization -- functionality 3
	 */
	//TODO
	
	
	
	/**======================================================
	 * Persona management -- functionality 4
	 */
	//TODO

	
	
	/**======================================================
	 * Prompt library -- functionality 5 
	 * hasPromptEditPermission() 
	 * addPrompt()
	 * updatePrompt()
	 */
	/**
	 * hasPromptEditPermissions()
	 * Determines whether the given user has sufficient role level within a workspace that allows them to edit Prompt Templates within that workspace
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
		String sql = "SELECT Role FROM UserWorkspace WHERE UserID = ? AND WorkspaceID = ?";
		try (PreparedStatement stmt = conn.prepareStatement(sql))
		{
			stmt.setInt(1, userId);
			stmt.setInt(2, workspaceId);
			try (ResultSet rs = stmt.executeQuery())
			{
				if (rs.next())
				{
					String role = rs.getString("Role");
					return "Admin".equals(role) || "Editor".equals(role);
				}
				return false;
			}
		}
	}

	/**
	 * addPrompt()
	 * Adds the specified prompt int the PromptTemplate table
	 * 
	 * @param int workspaceId -- the workspace's WorkspaceID value
	 * @param int userId -- the user's UserID value
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

		String insertPrompt = "INSERT INTO PromptTemplate (PromptID, Category, Visibility, Prompt) "
				+ "VALUES (prompt_seq.NEXTVAL, ?, ?, ?)";
		int newPromptId;
		try (PreparedStatement stmt = conn.prepareStatement(insertPrompt, new String[]
		{ "PromptID" }))
		{
			stmt.setString(1, category);
			stmt.setString(2, visibility);
			stmt.setString(3, promptText);
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
	 * updatePrompt()
	 * Updates the specified existing prompt int the PromptTemplate table
	 * 
	 * @param int promptId -- the PromptTemplate's PromptID value
	 * @param int userId -- the user's UserID value
	 * @param String newCategory -- the new value for the PromptTemplate's Category attribute
	 * @param String visibility -- the new value for the PromptTemplate's Visiblity attribute
	 * @param String promptText -- the new value for the PromptTemplate's Prompt attribute
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
		// Find which workspace owns this prompt
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

	
	
	/**======================================================
	 * Subscription tracking -- functionality 6
	 * updateUserSubscription()
	 * getUserDailyLimit()
	 * countUserMessagesToday()
	 * insertMessageIfWithinLimit()
	 */
	/**
	 * updateUserSubscription()
	 * Updates a user's subscription status -- which subscription tier their account is associated with
	 * 
	 * @param int userId -- the user's UserID value
	 * @param int newTierId -- the TierID value of the subscription tier that this user is being assigned
	 * 
	 * @return
	 * 
	 * @pre 
	 * @post
	 */
	public boolean updateUserSubscription(int userId, int newTierId) throws SQLException
	{
		String checkSql = "SELECT 1 FROM TierUser WHERE UserID = ?";
		boolean exists;
		try (PreparedStatement stmt = conn.prepareStatement(checkSql))
		{
			stmt.setInt(1, userId);
			try (ResultSet rs = stmt.executeQuery())
			{
				exists = rs.next();
			}
		}

		if (exists)
		{
			String updateSql = "UPDATE TierUser SET TierID = ? WHERE UserID = ?";
			try (PreparedStatement stmt = conn.prepareStatement(updateSql))
			{
				stmt.setInt(1, newTierId);
				stmt.setInt(2, userId);
				return stmt.executeUpdate() > 0;
			}
		}
		else
		{
			String insertSql = "INSERT INTO TierUser (UserID, TierID) VALUES (?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(insertSql))
			{
				stmt.setInt(1, userId);
				stmt.setInt(2, newTierId);
				return stmt.executeUpdate() > 0;
			}
		}
	}

	/**
	 * getUserDailyLimit()
	 * Gets the given user's limit on max messages per day based on their subscription tier
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
		String sql = "SELECT MAXMessgPerDay FROM MembershipTier m, TierUser t "
				+ "WHERE t.UserID = ? AND t.TierID = m.TierID";
		try (PreparedStatement stmt = conn.prepareStatement(sql))
		{
			stmt.setInt(1, userId);
			try (ResultSet rs = stmt.executeQuery())
			{
				if (rs.next())
					return rs.getInt("MAXMessgPerDay");
				else
					throw new SQLException("User does not have a subscription tier assigned");
			}
		}
	}

	/**
	 * countUserMessagesToday()
	 * Calculates the given user's number of messages sent today
	 * - For every tuple in Owns which has UserID = userId, use the matching ConversationID to...
	 * - Query the Contains relationship table for all tuples that have the matching ConversationID. For all of these tuples's MessageID values...
	 * - Query the Message table for tuples whose Time attribute is on today's date
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
		String sql = "SELECT COUNT(*) AS msg_count " + "FROM Message m "
				+ "JOIN Contains c ON m.MessageID = c.MessageID "
				+ "JOIN Conversation conv ON c.ConversationID = conv.ConversationID "
				+ "JOIN Owns o ON conv.ConversationID = o.ConversationID "
				+ "WHERE o.UserID = ? AND TRUNC(m.Time) = TRUNC(SYSDATE)";
		try (PreparedStatement stmt = conn.prepareStatement(sql))
		{
			stmt.setInt(1, userId);
			try (ResultSet rs = stmt.executeQuery())
			{
				if (rs.next())
					return rs.getInt("msg_count");
				return 0;
			}
		}
	}

	/**
	 * insertMessageIfWithinLimit()
	 * Checks the sender to see if they have reached their message limit for the day, if not, then add the message to the DB
	 * 
	 * @param int userId -- the user's UserID value
	 * @param int conversationId -- the containing Conversation's ConversationID value
	 * @param String senderRole -- the Role value of the sender (User) in the UserWorkspace relationship
	 * @param String promptText -- the new value for the PromptTemplate's Prompt attribute
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

			String insertMsg = "INSERT INTO Message (MessageID, SenderRole, Time, Content) "
					+ "VALUES (message_seq.NEXTVAL, ?, SYSDATE, ?)";
			int newMessageId;
			try (PreparedStatement stmt = conn.prepareStatement(insertMsg, new String[]
			{ "MessageID" }))
			{
				stmt.setString(1, senderRole);
				stmt.setString(2, content);
				stmt.executeUpdate();
				try (ResultSet rs = stmt.getGeneratedKeys())
				{
					if (rs.next())
						newMessageId = rs.getInt(1);
					else
						throw new SQLException("Failed to get MessageID");
				}
			}

			String linkMsg = "INSERT INTO Contains (MessageID, ConversationID) VALUES (?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(linkMsg))
			{
				stmt.setInt(1, newMessageId);
				stmt.setInt(2, conversationId);
				stmt.executeUpdate();
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
	
	
	
	/**======================================================
	 * Billing operations -- functionality 7
	 */
	//TODO
	
	
	
	/**======================================================
	 * Support Ticket Lifecycle -- functionality 8
	 */
	//TODO

	
	
	/**======================================================
	 * simple test ui menu -- replace with actual text based ui later on
	 */
	public void runTestMenu()
	{
		Scanner sc = new Scanner(System.in);
		while (true)
		{
			System.out.println("\n===== MENU =====");
			System.out.println("1. Add Prompt");
			System.out.println("2. Update Prompt");
			System.out.println("3. Update Subscription Tier");
			System.out.println("4. Insert Message (with limit check)");
			System.out.println("5. Exit");
			System.out.print("Choice: ");
			String choice = sc.nextLine().trim();

			try
			{
				switch (choice)
				{
				case "1":
					System.out.print("WorkspaceID: ");
					int ws = Integer.parseInt(sc.nextLine());
					System.out.print("UserID: ");
					int uid = Integer.parseInt(sc.nextLine());
					System.out.print("Category: ");
					String cat = sc.nextLine();
					System.out.print("Visibility (Public/Private): ");
					String vis = sc.nextLine();
					System.out.print("Prompt text: ");
					String txt = sc.nextLine();
					int newId = addPrompt(ws, uid, cat, vis, txt);
					System.out.println("Prompt added with ID: " + newId);
					break;
				case "2":
					System.out.print("PromptID: ");
					int pid = Integer.parseInt(sc.nextLine());
					System.out.print("UserID: ");
					int uid2 = Integer.parseInt(sc.nextLine());
					System.out.print("New Category: ");
					String cat2 = sc.nextLine();
					System.out.print("New Visibility: ");
					String vis2 = sc.nextLine();
					System.out.print("New Prompt text: ");
					String txt2 = sc.nextLine();
					boolean ok = updatePrompt(pid, uid2, cat2, vis2, txt2);
					System.out.println(ok ? "Updated" : "Update failed");
					break;
				case "3":
					System.out.print("UserID: ");
					int uid3 = Integer.parseInt(sc.nextLine());
					System.out.print("New TierID: ");
					int tid = Integer.parseInt(sc.nextLine());
					boolean ok2 = updateUserSubscription(uid3, tid);
					System.out.println(ok2 ? "Subscription updated" : "Update failed");
					break;
				case "4":
					System.out.print("UserID: ");
					int uid4 = Integer.parseInt(sc.nextLine());
					System.out.print("ConversationID: ");
					int cid = Integer.parseInt(sc.nextLine());
					System.out.print("SenderRole (user/assistant): ");
					String role = sc.nextLine();
					System.out.print("Message content: ");
					String msg = sc.nextLine();
					int msgId = insertMessageIfWithinLimit(uid4, cid, role, msg);
					if (msgId == -1)
						System.out.println("Daily message limit exceeded. Message rejected.");
					else
						System.out.println("Message inserted with ID: " + msgId);
					break;
				case "5":
					System.out.println("Exiting.");
					sc.close();
					return;
				default:
					System.out.println("Invalid choice");
				}
			}
			catch (Exception e)
			{
				System.err.println("Error: " + e.getMessage());
			}
		}
	}

	public static void main(String[] args)
	{
		LLMEcosystem app = new LLMEcosystem();
		app.runTestMenu();
		try
		{
			app.conn.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
}
