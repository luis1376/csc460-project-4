DROP TABLE UserBookmarks      CASCADE CONSTRAINTS PURGE;
DROP TABLE SpaceTemplates     CASCADE CONSTRAINTS PURGE;
DROP TABLE UserWorkspace      CASCADE CONSTRAINTS PURGE;
DROP TABLE Feedback           CASCADE CONSTRAINTS PURGE;
DROP TABLE SupportTicket      CASCADE CONSTRAINTS PURGE;
DROP TABLE SupportAgent       CASCADE CONSTRAINTS PURGE;
DROP TABLE Invoice            CASCADE CONSTRAINTS PURGE;
DROP TABLE PromptTemplate     CASCADE CONSTRAINTS PURGE;
DROP TABLE Message            CASCADE CONSTRAINTS PURGE;
DROP TABLE Conversation       CASCADE CONSTRAINTS PURGE;
DROP TABLE Persona            CASCADE CONSTRAINTS PURGE;
DROP TABLE Workspace          CASCADE CONSTRAINTS PURGE;
DROP TABLE Users              CASCADE CONSTRAINTS PURGE;
DROP TABLE MembershipTier     CASCADE CONSTRAINTS PURGE;

DROP SEQUENCE user_seq;
DROP SEQUENCE conv_seq;
DROP SEQUENCE msg_seq;
DROP SEQUENCE prompt_seq;
DROP SEQUENCE ticket_seq;
DROP SEQUENCE invoice_seq;
DROP SEQUENCE feedback_seq;
DROP SEQUENCE persona_seq;
DROP SEQUENCE workspace_seq;

-- sequences for auto-incrementing id attributes
CREATE SEQUENCE user_seq      START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE conv_seq      START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE msg_seq       START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE prompt_seq    START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE ticket_seq    START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE invoice_seq   START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE feedback_seq  START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE persona_seq   START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE workspace_seq START WITH 1000 INCREMENT BY 1;

--entities tables
CREATE TABLE MembershipTier (
    TierID        INTEGER PRIMARY KEY,
    TierName      VARCHAR2(15) NOT NULL,
    MaxMsgPerDay  INTEGER NOT NULL,
    Fee           NUMBER(10,2) NOT NULL,
    ProAccess     INTEGER DEFAULT 0 CHECK (ProAccess IN (0,1))  -- 0=no, 1=yes
);

-- Users because User is an Oracle keyword
CREATE TABLE Users (
    UserID        INTEGER PRIMARY KEY,
    UName          VARCHAR2(20) NOT NULL,
    Email         VARCHAR2(30) UNIQUE NOT NULL,
    PreferredUI   VARCHAR2(15),
    DateCreated   DATE NOT NULL,
    TierID        INTEGER NOT NULL REFERENCES MembershipTier(TierID)
);

CREATE TABLE Persona (
    PersonaID     INTEGER,
    VersionID     INTEGER,
    PName          VARCHAR2(20) NOT NULL,
    Guidelines    VARCHAR2(75),
    DateCreated   DATE NOT NULL,
    DeletedStatus INTEGER DEFAULT 0,   -- 0=active, 1=deleted
    PRIMARY KEY (PersonaID, VersionID)
);

CREATE TABLE Workspace (
    WorkspaceID   INTEGER PRIMARY KEY,
    WName          VARCHAR2(50) NOT NULL,
    Visibility    VARCHAR2(10) CHECK (Visibility IN ('shared','private')),
    DateCreated   DATE NOT NULL
);

CREATE TABLE Conversation (
    ConversationID INTEGER PRIMARY KEY,
    Title          VARCHAR2(50),
    DateCreated    DATE NOT NULL,
    VersionID      INTEGER,
    PersonaID      INTEGER,
    UserID         INTEGER NOT NULL REFERENCES Users(UserID) ON DELETE CASCADE,
    WorkspaceID    INTEGER REFERENCES Workspace(WorkspaceID) ON DELETE SET NULL,
    IsActive       INTEGER,
    FOREIGN KEY (PersonaID, VersionID) REFERENCES Persona(PersonaID, VersionID)
);

CREATE TABLE Message (
    MessageID      INTEGER PRIMARY KEY,
    SenderRole     VARCHAR2(10) CHECK (SenderRole IN ('User','AI')),
    MTime           DATE NOT NULL,
    Content        VARCHAR2(4000) NOT NULL,
    ConversationID INTEGER NOT NULL REFERENCES Conversation(ConversationID) ON DELETE CASCADE
);

CREATE TABLE Feedback (
    FeedbackID    INTEGER PRIMARY KEY,
    IsThumbsUp    INTEGER NOT NULL CHECK (IsThumbsUp IN (0,1)),   -- 1="thumbs up", 0="thumbs down"
    FText          VARCHAR2(500),
    FDate          DATE NOT NULL,
    UserID        INTEGER NOT NULL REFERENCES Users(UserID),
    MessageID     INTEGER NOT NULL UNIQUE REFERENCES Message(MessageID)
);

CREATE TABLE PromptTemplate (
    PromptID      INTEGER PRIMARY KEY,
    Category      VARCHAR2(20),
    Visibility    VARCHAR2(10) CHECK (Visibility IN ('private','shared')),
    Prompt        VARCHAR2(250) NOT NULL,
    UserID        INTEGER NOT NULL REFERENCES Users(UserID)
);

CREATE TABLE Invoice (
    InvoiceID     INTEGER PRIMARY KEY,
    UserID        INTEGER NOT NULL REFERENCES Users(UserID) ON DELETE CASCADE,
    Amount        NUMBER(10,2) NOT NULL,
    IDate         DATE NOT NULL,
    IStatus       VARCHAR2(10) CHECK (IStatus IN ('paid','unpaid'))
);

CREATE TABLE SupportAgent (
    AgentID       INTEGER PRIMARY KEY,
    SName          VARCHAR2(20) NOT NULL
);

CREATE TABLE SupportTicket (
    TicketID      INTEGER PRIMARY KEY,
    UserID        INTEGER NOT NULL REFERENCES Users(UserID),
    AgentID       INTEGER NOT NULL REFERENCES SupportAgent(AgentID),
    Topic         VARCHAR2(50) NOT NULL,
    DateOpened    DATE NOT NULL,
    DateClosed    DATE,
    STStatus      VARCHAR2(15) CHECK (STStatus IN ('Resolved','Escalated'))
);

-- relationship tables
CREATE TABLE SpaceTemplates (
    WorkspaceID   INTEGER REFERENCES Workspace(WorkspaceID) ON DELETE CASCADE,
    PromptID      INTEGER REFERENCES PromptTemplate(PromptID) ON DELETE CASCADE,
    PRIMARY KEY (WorkspaceID, PromptID)
);

CREATE TABLE UserWorkspace (
    UserID        INTEGER REFERENCES Users(UserID) ON DELETE CASCADE,
    WorkspaceID   INTEGER REFERENCES Workspace(WorkspaceID) ON DELETE CASCADE,
    DateJoined    DATE NOT NULL,
    UWRole        VARCHAR2(20) CHECK (UWRole IN ('Admin','Editor','Viewer')),
    PRIMARY KEY (UserID, WorkspaceID)
);

CREATE TABLE UserBookmarks (
    UserID        INTEGER REFERENCES Users(UserID) ON DELETE CASCADE,
    MessageID     INTEGER REFERENCES Message(MessageID) ON DELETE CASCADE,
    PRIMARY KEY (UserID, MessageID)
);

-- necessary pre-population of membership tiers and support agents
INSERT INTO MembershipTier (TierID, TierName, MaxMsgPerDay, Fee, ProAccess) VALUES (1, 'Free', 10, 0.00, 0);
INSERT INTO MembershipTier (TierID, TierName, MaxMsgPerDay, Fee, ProAccess) VALUES (2, 'Plus', 100, 9.99, 1);
INSERT INTO MembershipTier (TierID, TierName, MaxMsgPerDay, Fee, ProAccess) VALUES (3, 'Enterprise', 1000, 25.99, 1);

INSERT INTO SupportAgent (AgentID, SName) VALUES (1, 'Bob');
INSERT INTO SupportAgent (AgentID, SName) VALUES (2, 'Karen');
INSERT INTO SupportAgent (AgentID, SName) VALUES (3, 'John');
