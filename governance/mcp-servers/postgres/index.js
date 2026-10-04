import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import pg from "pg";

const pool = new pg.Pool({
  host: process.env.DB_HOST ?? "localhost",
  port: Number(process.env.DB_PORT ?? 5432),
  user: process.env.DB_USER ?? "postgres",
  password: process.env.DB_PASSWORD ?? "postgres",
  database: process.env.DB_NAME ?? "erp_db",
});

const server = new Server(
  { name: "erp-postgres-mcp", version: "1.0.0" },
  { capabilities: { tools: {} } },
);

const TOOLS = [
  {
    name: "query",
    description:
      "Run a SQL statement against the erp_db Postgres database. Supports SELECT, INSERT, UPDATE, DELETE and DDL. Returns the resulting rows (if any) and row count.",
    inputSchema: {
      type: "object",
      properties: {
        sql: { type: "string", description: "The SQL statement to execute" },
        params: {
          type: "array",
          description: "Optional positional parameters for $1, $2, ... placeholders",
          items: {},
        },
      },
      required: ["sql"],
    },
  },
  {
    name: "list_tables",
    description: "List all tables in the public schema.",
    inputSchema: { type: "object", properties: {} },
  },
  {
    name: "describe_table",
    description: "List columns, types and nullability for a given table.",
    inputSchema: {
      type: "object",
      properties: {
        table: { type: "string", description: "Table name (case-insensitive)" },
      },
      required: ["table"],
    },
  },
];

server.setRequestHandler(ListToolsRequestSchema, async () => ({ tools: TOOLS }));

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    if (name === "query") {
      const result = await pool.query(args.sql, args.params ?? []);
      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(
              { rowCount: result.rowCount, rows: result.rows },
              null,
              2,
            ),
          },
        ],
      };
    }

    if (name === "list_tables") {
      const result = await pool.query(
        `SELECT table_name FROM information_schema.tables
         WHERE table_schema = 'public' ORDER BY table_name`,
      );
      return {
        content: [{ type: "text", text: JSON.stringify(result.rows, null, 2) }],
      };
    }

    if (name === "describe_table") {
      const result = await pool.query(
        `SELECT column_name, data_type, is_nullable, column_default
         FROM information_schema.columns
         WHERE table_schema = 'public' AND lower(table_name) = lower($1)
         ORDER BY ordinal_position`,
        [args.table],
      );
      return {
        content: [{ type: "text", text: JSON.stringify(result.rows, null, 2) }],
      };
    }

    return {
      isError: true,
      content: [{ type: "text", text: `Unknown tool: ${name}` }],
    };
  } catch (err) {
    return {
      isError: true,
      content: [{ type: "text", text: `Error: ${err.message}` }],
    };
  }
});

const transport = new StdioServerTransport();
await server.connect(transport);
