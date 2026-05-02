-- MAIN ENTITY TABLES

CREATE TABLE User (
    UserID        integer PRIMARY KEY,
    Name          varchar2(20),
    Email         varchar2(30),
    preferredUI   varchar2(15),   -- Language, like English
    DateCreated   DATE,          -- Of account creation
    TierID        integer FOREIGN KEY
);

CREATE TABLE Conversation (
  ConversationID  integer PRIMARY KEY,
  title           varchar2(50),
  DateCreated     DATE,
  versionID       integer FOREIGN KEY,
  personaID       integer FOREIGN KEY,
  userID          integer FOREIGN KEY,
  workspaceID     integer FOREIGN KEY,
);

CREATE_TABLE Message (
  MessageID       integer PRIMARY KEY,
  SenderRole      varchar2(10), -- "User" or "AI"
  Time            DATE,
  Content         varchar2(1000),
  ConversationID  integer FOREIGN KEY
);

CREATE TABLE Feedback (
  FeedbackID   integer PRIMARY KEY,
  Rating       integer,          -- 1-10?
  Text         varchar2(500), -- optional
  Date         DATE,
  userID       integer FOREIGN KEY,
  MessageID    integer FOREIGN KEY
);

CREATE TABLE Persona (
  PersonaID      integer,
  Name           varchar2(20),
  Guidelines     varchar2(75), -- eg. "Act as a techincal writer"
  DateCreated    DATE,
  VersionID      integer,
  deletedStatus  integer          -- 0 = active, > 0 is deleted
  PRIMARY KEY (PersonaID, versionID)
);

CREATE TABLE Invoice (
  InvoiceID      integer PRIMARY KEY,
  UserID         integer FOREIGN KEY,
  Amount         float(2),   --  to 2 decimal places for cents
  Date           DATE,
  status         varchar2(10) -- "paid" or "unpaid"
);

CREATE TABLE PromptTemplate (
  PromptID       integer PRIMARY KEY,
  Category       varchar2(20),   -- user can categorize templates
  Visibility     varchar2(10),   -- "private" or "shared" within a workspace
  Prompt         varchar2(250),
  userID         integer FOREIGN KEY
);

CREATE TABLE Workspace (
  WorskpaceID    integer PRIMARY KEY,
  Name           varchar2(50),
  Visibility     varchar2(10),  -- "shared" or "private"
  DateCreated    DATE
);

CREATE TABLE BillingProfile (
  ProfileID      integer PRIMARY KEY,
  billingAdress  varchar2(50),
  PaymentMethod  varchar2(20),
  userID         integer FOREIGN KEY
);

CREATE TABLE MembershipTier (
  TierID         integer PRIMARY KEY,
  TierName       varchar2(15), -- "Free", "Plus", or "Enterprise"
  MaxMsgPerDay   integer,
  Fee            float(2),
  ProAccess      integer -- 1 or 0, if tier allows pro models to be used
);

CREATE TABLE SupportTicket (
  TicketID       integer PRIMARY KEY,
  UserID         integer FOREIGN KEY,
  AgentID        integer FOREIGN KEY
  Topic          varchar2(50),
  DateOpened     DATE,
  DateClosed     DATE,
  Status         varchar2(15) -- "Resolved" or "Escalated"
);

CREATE TABLE SupportAgent (
  AgentID       integer PRIMARY KEY,
  Name          varchar2(20)
);


-- RELATIONAL TABLES, for the many to many relationships

CREATE TABLE UserWorkspace (
  UserID        integer,
  WorkspaceID   integer,
  DateJoined    DATE,
  Role          varchar2(20),
);

CREATE TABLE spaceTemplates (
  WorkspaceID  integer,
  PromptId     integer
);

CREATE TABLE userBookmarks (
  UserID       integer,
  MessageId    integer
);
