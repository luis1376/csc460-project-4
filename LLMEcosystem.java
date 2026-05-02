
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

	/**
	 * ====================================================== 
	 * Manage user accounts -- functionality 1
	 */
	// TODO

	/**
	 * ====================================================== 
	 * Handle Conversations & messages -- functionality 2
	 */
	// TODO

	/**
	 * ====================================================== 
	 * workspace organization -- functionality 3
	 */
	// TODO

	/**
	 * ====================================================== 
	 * Persona management -- functionality 4
	 */
	// TODO

	/**
	 * ====================================================== 
	 * Prompt library -- functionality 5 
	 * - hasPromptEditPermission() 
	 * - addPrompt() 
	 * - updatePrompt()
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
	public int addPrompt(int workspaceId, int userId, String category, String visibility, String promptText)throws SQLException
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
	public boolean updatePrompt(int promptId, int userId, String newCategory, String newVisibility, String newPromptText) throws SQLException
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

	/**
	 * ====================================================== 
	 * Subscription tracking -- functionality 6 
	 * - updateUserSubscription() 
	 * - getUserDailyLimit()
	 * - countUserMessagesToday() 
	 * - insertMessageIfWithinLimit()
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
				+ "WHERE c.UserID = ? AND TRUNC(m.Time) = TRUNC(SYSDATE)";
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
	public int insertMessageIfWithinLimit(int userId, int conversationId, String senderRole, String content) throws SQLException
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

			String insertMsg = "INSERT INTO Message (MessageID, SenderRole, Time, Content, ConversationID) "
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
	 * ====================================================== 
	 * Billing operations -- functionality 7
	 */
	// TODO

	/**
	 * ====================================================== 
	 * Support Ticket Lifecycle -- functionality 8
	 */
	// TODO

	/**
	 * ====================================================== 
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