
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

	// private static final String DB_URL = "jdbc:oracle:thin:@aloe.cs.arizona.edu:1521:oracle";
	// private static final String DB_USER = "shalevancleve";
	// private static final String DB_PASS = "a2532";

	private static Connection conn;
	private Scanner scanner;

	public LLMEcosystem()
	{
		scanner = new Scanner(System.in);
		connectToDatabase();
	}

	private void connectToDatabase()
	{
		System.out.print("Oracle username: ");
		String user = scanner.nextLine().trim();
		System.out.print("Password: ");
		String pass = scanner.nextLine().trim();

		String url = "jdbc:oracle:thin:@aloe.cs.arizona.edu:1521:oracle";
		try
		{
			Class.forName("oracle.jdbc.OracleDriver");
			conn = DriverManager.getConnection(url, user, pass);
			System.out.println("Connected to Oracle DB successfully.\n");
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
	public void addUser(String name, String email, String preferredUI) {
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
	public void updateUser(int UserID, String newName, String newEmail, String preferredUI, int tierID) throws SQLException {
		String updateQuery = "UPDATE Users SET Name = ?, Email = ?, preferredUI = ?, tierID = ? WHERE UserID = ?";
		PreparedStatement stmt = conn.prepareStatement(updateQuery);
		stmt.setString(1,newName); 
		stmt.setString(2, newEmail);
		stmt.setString(3, preferredUI);
		stmt.setInt(4, tierID);
		stmt.setInt(5, UserID);
		stmt.executeUpdate();
	}


	// checks if a user has unpaid invoices or an unclosed support ticket
	public boolean checkUnpaidInvoicesOrSupportTickets(int UserID) throws SQLException {
		String query = "SELECT DISTINCT UserID FROM SupportTicket WHERE UserID = ? AND DateClosed IS NULL" +
					   " UNION " + 
					   "SELECT DISTINCT UserID FROM Invoice WHERE UserID = ? AND status = 'unpaid'";
		PreparedStatement stmt = conn.prepareStatement(query);
		stmt.setInt(1, UserID);
		stmt.setInt(2, UserID);
		ResultSet rs = stmt.executeQuery();
		return rs.next(); // if there is a next, it means userID was found in either condition
	}
	
	/*
	* Deletes a user in the database
	*
	*
	* @note deletion fails if invoices unpaid or open support tickets
	*/
	public void deleteUser(int UserID) throws SQLException {
		boolean failed = checkUnpaidInvoicesOrSupportTickets(UserID);
		if(failed) {
			System.out.println("Cannot delete user account: invoice is unpaid or a support ticket is open");
			return;
		}
		String deleteStmt = "DELETE FROM Users WHERE UserID = ?";
		PreparedStatement stmt = conn.prepareStatement(deleteStmt);
		stmt.setInt(1, UserID);
		stmt.executeUpdate();
	}

	/**
	 * ====================================================== 
	 * Handle Conversations & messages -- functionality 2
	 */
	// TODO
	private void startConversation()
	{
		System.out.println("TODO: startConversation");
	}

	private void addMessageToConversation()
	{
		System.out.println("TODO: addMessageToConversation");
	}

	private void updateMessageFeedback()
	{
		System.out.println("TODO: updateMessageFeedback");
	}

	
	
	/**
	 * ====================================================== 
	 * workspace organization -- functionality 3
	 */
	//
	private int getAvailableWorkspaceID(){
		//get next ID from sequence
		String sq = "SELECT workspace_seq.NEXTVAL from dual";
		try{
			Statement s = conn.createStatement();
			ResultSet r = s.executeQuery(sq);
			
			//get the number
			if(r.next()){
				int nexID = r.getInt(1);
				r.close();
				s.close();
				return nexID;
			}

			r.close();
			s.close();
		} catch(SQLException e){
			System.err.println("Error getting next available WorkspaceID");
		}

		return -1;
	}

	private void createWorkspace(){
		try{
			System.out.println("Enter userID of the creator:");
			int userID = Integer.parseInt(scanner.nextLine().trim());

			System.out.println("Enter Workspace name:");
			String name = scanner.nextLine().trim();

			System.out.println("Enter Visibility:");
			String visibility = scanner.nextLine().trim();

			if(!visibility.equals("shared")&& 
						!visibility.equals("private")){
				System.out.println("Options for Visibility are: shared, private");
			
				return;
			}
			int workspaceID = getAvailableWorkspaceID();

			if(workspaceID == -1){
				System.out.println("Something went wrong creating workspace as workspaceID " +
							"was not generated");
				
				return;
			}
			
			//we'll do two inserts, so either both work or none do.
			conn.setAutoCommit(false);

			//first one
			String insertWorkspace = "INSERT INTO Workspace (WorkspaceID, Name, Visibility, DateCreated) " +
										"VALUES(?, ?, ?, CURRENT_DATE)";
			PreparedStatement st1 = conn.prepareStatement(insertWorkspace);

			//? workspaceID
			st1.setInt(1, workspaceID);
			
			//? name
			st1.setString(2, name);

			//? visibility
			st1.setString(3, visibility);

			st1.executeUpdate();

			st1.close();

			//second one
			String insertUserWorkspace = "INSERT INTO UserWorkspace (UserID, WorkspaceID, DateJoined, Role) " +
										"VALUES(?, ?, CURRENT_DATE, 'Admin')";

			PreparedStatement st2 = conn.prepareStatement(insertUserWorkspace);

			//? userID
			st2.setInt(1, userID);

			//? workspaceID
			st2.setInt(2, workspaceID);
			
			st2.executeUpdate();
			st2.close();

			conn.commit();

			//reactivate autocomit
			conn.setAutoCommit(true);
			
			System.out.println("Workspace created. WorkspaceID: " + workspaceID);
			System.out.println("UserID: " + userID);
		
		//user did not use numbers
		} catch(NumberFormatException e){
			System.out.println("IDs must be integers");
	
		} catch(SQLException e){

			try{
				//an insert did not work, so go back in the changes
				conn.rollback();

				conn.setAutoCommit(true);
			}

			catch(SQLException rollingbackE){
				System.err.println("error rolling back.");
			}

			System.err.println("Error creating the workspace.");
		}
	}

	private void modifyWorkspace(){
		try{
			System.out.println("Enter WorkspaceId to modify:");
			int wsID = Integer.parseInt(scanner.nextLine().trim());
			
			System.out.println("Enter the new Workspace name:");
			String newName = scanner.nextLine().trim();
			
			System.out.println("Enter the new Visibility:");
			String newV = scanner.nextLine().trim();
			
			if(!newV.equals("shared")&& !newV.equals("private")){
				System.out.println("Options for Visibility are: shared, private");
				return;
			}

			String updateWorkspace = 
				"UPDATE Workspace " + 
				"SET Name = ?, Visibility = ? " +
				"WHERE WorkspaceID = ?";

			PreparedStatement st = conn.prepareStatement(updateWorkspace);

			//? = new name
			st.setString(1, newName);

			//? = new Visibility
			st.setString(2, newV);

			//? = workspaceId to modify
			st.setInt(3, wsID);

			int rowschanged = st.executeUpdate();

			st.close();
			//if something was changed, it did modified workspace
			if(rowschanged > 0){
				System.out.println("Workspace modified.");
			}
			else{
				System.out.println("Workspace not found.");
			}

		}catch(NumberFormatException e){
			System.out.println("workspaceID must be an integer.");
		
		} catch(SQLException e){
			System.err.println("Something went wrong modifying the workspace.");
		}
	}

	private boolean userBelongsWorkspace(int UserID, int WorkspaceID){
		try{
			String checkUserWorkspace = "SELECT COUNT(*) AS Total From UserWorkspace " +
			"WHERE UserID = ? AND WorkspaceID = ?";
			
			PreparedStatement st = conn.prepareStatement(checkUserWorkspace);

			//? = UserID being checked
			st.setInt(1, UserID);

			//? = WorkspaceID being checked
			st.setInt(2, WorkspaceID);

			ResultSet r = st.executeQuery();

			if(r.next()){
				int total = r.getInt("Total");

				r.close();
				st.close();
				
				//user belongs if total > 0
				boolean result = total > 0;

				return result;
			}

			r.close();
			st.close();
		}

		catch(SQLException e){
			System.err.println("Something went wrong checking the relationship UserWorkspace.");
		}

		return false;
	}

	private void moveConversationToWorkspace() {
		try{
			System.out.println("Enter UserID:");
			int userID = Integer.parseInt(scanner.nextLine().trim());

			System.out.println("Enter conversationID:");
			int conversationID = Integer.parseInt(scanner.nextLine().trim());

			System.out.println("Enter WorkspaceID:");
			int wsID = Integer.parseInt(scanner.nextLine().trim());

			if(!userBelongsWorkspace(userID, wsID)){
				System.out.println("User " + userID + " does not belong to this workspace.");
				return;
			}

			String moveConversation = 
			"UPDATE Conversation " +
			"SET WorkspaceID = ? " +
			"WHERE ConversationID = ? AND UserID = ?";

			PreparedStatement st = conn.prepareStatement(moveConversation);

			//? = wsID
			st.setInt(1, wsID);

			//? = conversation to move
			st.setInt(2, conversationID);

			//? = userID
			st.setInt(3, userID);

			int rows = st.executeUpdate();

			st.close();

			if(rows > 0){
				System.out.println("Success moving conversation to workspace");
			}

			else{
				System.out.println("conversation does not exist or does not belong to user "
					+ userID + ".");
			}
		} catch(NumberFormatException e){
			System.out.println("IDs must be integers.");
		
		} catch(SQLException e){
			System.err.println("Something went wrong moving the conversation.");
		}
	}

	/**
	 * ====================================================== 
	 * Persona management -- functionality 4
	 */
	// TODO
	private void createPersona()
	{
		System.out.println("TODO: createPersona");
	}

	private void deletePersona()
	{
		System.out.println("TODO: deletePersona (check active conversations)");
	}

	
	
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

	/**
	 * generateInvoice() Creates a new unpaid invoice for a user based on their
	 * current membership tier fee.
	 */
	private void generateInvoice() throws SQLException
	{
		System.out.print("Enter UserID: ");
		int userId = Integer.parseInt(scanner.nextLine().trim());

		// look up the user's tier fee
		String feeSql = "SELECT mt.Fee FROM Users u "
				+ "JOIN MembershipTier mt ON u.TierID = mt.TierID "
				+ "WHERE u.UserID = ?";
		double fee;
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

		String insertSql = "INSERT INTO Invoice (InvoiceID, UserID, Amount, \"Date\", Status) "
				+ "VALUES (invoice_seq.NEXTVAL, ?, ?, SYSDATE, 'unpaid')";
		try (PreparedStatement stmt = conn.prepareStatement(insertSql, new String[]{ "InvoiceID" }))
		{
			stmt.setInt(1, userId);
			stmt.setDouble(2, fee);
			stmt.executeUpdate();
			try (ResultSet rs = stmt.getGeneratedKeys())
			{
				if (rs.next())
					System.out.printf("Invoice generated (ID: %d) for $%.2f — status: unpaid%n",
							rs.getInt(1), fee);
			}
		}
	}

	/**
	 * markInvoicePaid() Marks an existing invoice as "paid."
	 */
	private void markInvoicePaid() throws SQLException
	{
		System.out.print("Enter InvoiceID: ");
		int invoiceId = Integer.parseInt(scanner.nextLine().trim());

		String sql = "UPDATE Invoice SET Status = 'paid' WHERE InvoiceID = ? AND Status = 'unpaid'";
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
	 * ======================================================
	 * Support Ticket Lifecycle -- functionality 8
	 */

	/**
	 * createSupportTicket() Opens a new support ticket for a user and assigns it
	 * to an agent.
	 */
	private void createSupportTicket() throws SQLException
	{
		// show available agents
		String agentSql = "SELECT AgentID, Name FROM SupportAgent ORDER BY AgentID";
		try (PreparedStatement stmt = conn.prepareStatement(agentSql);
			 ResultSet rs = stmt.executeQuery())
		{
			System.out.println("\nAvailable agents:");
			while (rs.next())
				System.out.printf("  %d - %s%n", rs.getInt("AgentID"), rs.getString("Name"));
		}

		System.out.print("Enter UserID: ");
		int userId = Integer.parseInt(scanner.nextLine().trim());
		System.out.print("Enter Topic: ");
		String topic = scanner.nextLine().trim();
		System.out.print("Enter AgentID: ");
		int agentId = Integer.parseInt(scanner.nextLine().trim());

		String insertSql = "INSERT INTO SupportTicket (TicketID, UserID, AgentID, Topic, DateOpened) "
				+ "VALUES (ticket_seq.NEXTVAL, ?, ?, ?, SYSDATE)";
		try (PreparedStatement stmt = conn.prepareStatement(insertSql, new String[]{ "TicketID" }))
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

	/**
	 * assignTicketToAgent() Reassigns an existing open ticket to a different agent.
	 */
	private void assignTicketToAgent() throws SQLException
	{
		System.out.print("Enter TicketID: ");
		int ticketId = Integer.parseInt(scanner.nextLine().trim());

		// show available agents
		String agentSql = "SELECT AgentID, Name FROM SupportAgent ORDER BY AgentID";
		try (PreparedStatement stmt = conn.prepareStatement(agentSql);
			 ResultSet rs = stmt.executeQuery())
		{
			System.out.println("\nAvailable agents:");
			while (rs.next())
				System.out.printf("  %d - %s%n", rs.getInt("AgentID"), rs.getString("Name"));
		}

		System.out.print("Enter new AgentID: ");
		int agentId = Integer.parseInt(scanner.nextLine().trim());

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

	/**
	 * updateTicketResolution() Closes a ticket with a resolution status and
	 * displays the resolution duration.
	 */
	private void updateTicketResolution() throws SQLException
	{
		System.out.print("Enter TicketID: ");
		int ticketId = Integer.parseInt(scanner.nextLine().trim());
		System.out.print("Enter resolution status (Resolved/Escalated): ");
		String status = scanner.nextLine().trim();
		if (!status.equals("Resolved") && !status.equals("Escalated"))
		{
			System.out.println("Status must be 'Resolved' or 'Escalated'.");
			return;
		}

		String updateSql = "UPDATE SupportTicket SET Status = ?, DateClosed = SYSDATE "
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

		// display duration in days
		String durSql = "SELECT TRUNC(DateClosed) - TRUNC(DateOpened) AS DurationDays "
				+ "FROM SupportTicket WHERE TicketID = ?";
		try (PreparedStatement stmt = conn.prepareStatement(durSql))
		{
			stmt.setInt(1, ticketId);
			try (ResultSet rs = stmt.executeQuery())
			{
				if (rs.next())
					System.out.printf("Ticket closed as '%s'. Resolution duration: %d day(s).%n",
							status, rs.getInt("DurationDays"));
			}
		}
	}

	
	
	/*
	 * Required queries
	 * - queryBookmarkedMessages()
	 * - queryUnpaidInvoices()
	 * - queryMostHelpfulPersona()
	 * - queryWorkspaceMembers()
	 */
	/**
	 * queryBookmarkedMessages()
	 * For a given User, list all their Bookmarked messages across all conversations, including the conversation title and the timestamp.
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
			SELECT c.Title, m.Time, m.Content
			FROM UserBookmarks ub
			JOIN Message m ON ub.MessageID = m.MessageID
			JOIN Conversation c ON m.ConversationID = c.ConversationID
			WHERE ub.UserID = ?
			ORDER BY m.Time DESC
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
							rs.getTimestamp("Time"), rs.getString("Content"));
				}
				if (!found)
					System.out.println("No bookmarked messages for this user.");
			}
		}
	}

	/**
	 * queryUnpaidInvoices()
	 * List all users who have “Unpaid” invoices, including their email, the total amount owed, and the date of their last conversation.
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
			WHERE i.Status = 'unpaid'
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
	 * queryMostHelpfulPersona()
	 * Identify the “Most Helpful” Persona: List the persona name that has received the highest percentage of “Thumbs Up” feedback across all conversations linked to it
	 * Note: feedback table stores Rating 1-10 so for now I'm just considering "Thumbs Up" as Rating >= 8
	 * 
	 * @return N/A
	 * 
	 * @pre
	 * @post
	 */
	private void queryMostHelpfulPersona() throws SQLException
	{
		String sql = """
			SELECT p.Name,
       			COUNT(f.FeedbackID) AS TotalFeedback,
       			SUM(f.IsThumbsUp) AS ThumbsUpCount,
       			ROUND(100.0 * SUM(f.IsThumbsUp) / COUNT(f.FeedbackID), 2) AS Percentage
			FROM Persona p
			JOIN Conversation c ON p.PersonaID = c.PersonaID AND p.VersionID = c.VersionID
			JOIN Message m ON c.ConversationID = m.ConversationID
			JOIN Feedback f ON m.MessageID = f.MessageID
			WHERE p.DeletedStatus = 0
			GROUP BY p.Name
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
						rs.getString("Name"), rs.getDouble("Percentage"), rs.getInt("TotalFeedback"));
			}
			else
			{
				System.out.println("No feedback data available.");
			}
		}
	}

	/**
	 * queryWorkspaceMembers()
	 * Additional non-trivial query of our own design -- List workspaces where a given user has Admin role, along with the number of members in that workspace.
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
			SELECT w.Name AS WorkspaceName, COUNT(uw2.UserID) AS MemberCount
			FROM Workspace w
			JOIN UserWorkspace uwAdmin ON w.WorkspaceID = uwAdmin.WorkspaceID
			LEFT JOIN UserWorkspace uw2 ON w.WorkspaceID = uw2.WorkspaceID
			WHERE uwAdmin.UserID = ? AND uwAdmin.Role = 'Admin'
			GROUP BY w.Name
			ORDER BY w.Name
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
	 * Current UI for testing -- likely will need changing with other functionality
	 * implementations
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
					//ignore 
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
				 System.out.print("New Name (leave blank to keep current): ");
				 String newName = scanner.nextLine();
				 System.out.print("New Email (leave blank to keep current): ");
				 String newEmail = scanner.nextLine();
				 System.out.print("New Preferred UI (leave blank to keep current): ");
				 String newUI = scanner.nextLine();
				 System.out.print("New TierID (leave blank to keep current): ");
				 String tierInput = scanner.nextLine();
				 int newTierId = tierInput.isEmpty() ? -1 : Integer.parseInt(tierInput);
				 updateUser(userId, newName, newEmail, newUI, newTierId);
				 System.out.println("User updated successfully.");
				break;
			case "3":
				 System.out.print("UserID to delete: ");
				 int delUserId = Integer.parseInt(scanner.nextLine());
				 deleteUser(delUserId);
				 System.out.println("User deleted successfully (if no unpaid invoices or open support tickets).");
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
		System.out.println("2. Add Message to Conversation");
		System.out.println("3. Update Message Feedback");
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
				addMessageToConversation();
				break;
			case "3":
				updateMessageFeedback();
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
		System.out.println("2. Insert Message (with daily limit check)");
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
			else if (sub.equals("2"))
			{
				System.out.print("UserID: ");
				int uid = Integer.parseInt(scanner.nextLine());
				System.out.print("ConversationID: ");
				int cid = Integer.parseInt(scanner.nextLine());
				System.out.print("SenderRole (User/AI): ");
				String role = scanner.nextLine();
				if (!role.equals("User") && !role.equals("AI"))
				{
					System.out.println("SenderRole must be 'User' or 'AI'.");
					return;
				}
				System.out.print("Message content: ");
				String msg = scanner.nextLine();
				int msgId = insertMessageIfWithinLimit(uid, cid, role, msg);
				if (msgId == -1)
					System.out.println("Daily message limit exceeded. Message rejected.");
				else
					System.out.println("Message inserted with ID: " + msgId);
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
