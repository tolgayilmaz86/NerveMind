package ai.nervemind.app.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Root controller for the NerveMind API.
 * Provides basic information and redirects for authenticated users.
 */
@RestController
public class RootController {

    /**
     * Root endpoint that provides API information as HTML.
     *
     * @return HTML response with API information
     */
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> root() {
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>NerveMind API Server</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            margin: 40px;
                            background-color: #f5f5f5;
                        }
                        .container {
                            max-width: 800px;
                            margin: 0 auto;
                            background: white;
                            padding: 30px;
                            border-radius: 8px;
                            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                        }
                        h1 {
                            color: #2c3e50;
                            border-bottom: 2px solid #3498db;
                            padding-bottom: 10px;
                        }
                        .endpoint {
                            background: #ecf0f1;
                            padding: 10px;
                            margin: 5px 0;
                            border-radius: 4px;
                        }
                        .endpoint strong { color: #2c3e50; }
                        .status { color: #27ae60; font-weight: bold; }
                        a { color: #3498db; text-decoration: none; }
                        a:hover { text-decoration: underline; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div style="text-align: center; margin-bottom: 20px;">
                            <img src="/images/logo-trans.png" alt="NerveMind Logo"
                                 style="max-width: 150px; height: auto;">
                        </div>
                        <h1>NerveMind API Server</h1>
                        <p><strong>Status:</strong> <span class="status">✅ Running</span></p>
                        <p><strong>Version:</strong> 1.0.0</p>

                        <h2>Available Endpoints</h2>
                        <div class="endpoint">
                            <strong>Workflows:</strong>
                            <a href="/api/workflows" target="_blank">/api/workflows</a><br>
                            <em>Manage workflow definitions and configurations</em>
                        </div>
                        <div class="endpoint">
                            <strong>Executions:</strong>
                            <a href="/api/executions" target="_blank">/api/executions</a><br>
                            <em>Monitor and control workflow executions</em>
                        </div>
                        <div class="endpoint">
                            <strong>Credentials:</strong>
                            <a href="/api/credentials" target="_blank">/api/credentials</a><br>
                            <em>Manage API keys and authentication credentials</em>
                        </div>
                        <div class="endpoint">
                            <strong>H2 Database Console:</strong>
                            <a href="/h2-console" target="_blank">/h2-console</a><br>
                            <em>Database administration interface</em>
                        </div>

                        <h2>Desktop Application</h2>
                        <p>This API server is part of the NerveMind desktop application. For the full
                           visual workflow designer experience, please run the desktop application
                           instead.</p>

                        <p><em>🔒 You are authenticated and can access all API endpoints.</em></p>
                    </div>
                </body>
                </html>
                """;

        return ResponseEntity.ok(html);
    }
}