package com.styledesk;

import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@Path("/")
public class StyleDeskServer {

    private static final String BASE_URL =
        "https://styledesk-agent-246551819686.us-central1.run.app";

    private final InMemoryRunner runner =
        new InMemoryRunner(StyleDeskAgent.ROOT_AGENT);

    // Per-user sessions — enables multi-turn conversation memory
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /** Sofia chat UI */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response index() {
        String html =
            "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "  <meta charset=\"UTF-8\">\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "  <link rel=\"icon\" type=\"image/svg+xml\" href=\"/favicon.svg\">\n" +
            "  <link rel=\"shortcut icon\" href=\"/favicon.svg\">\n" +
            "  <title>StyleDesk — Chat with Sofia</title>\n" +
            "  <link href=\"https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,400;0,600;1,400&family=DM+Sans:wght@300;400;500&display=swap\" rel=\"stylesheet\">\n" +
            "  <style>\n" +
            "    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }\n" +
            "    :root {\n" +
            "      --cream: #faf7f4;\n" +
            "      --warm-white: #fff9f5;\n" +
            "      --blush: #e8c4b8;\n" +
            "      --rose: #d4847a;\n" +
            "      --deep-rose: #b5635a;\n" +
            "      --navy: #1b2a4a;\n" +
            "      --navy-light: #2d3f63;\n" +
            "      --sand: #e8e0d5;\n" +
            "      --sand-light: #f0ece8;\n" +
            "      --text-muted: #8a7f78;\n" +
            "    }\n" +
            "    html, body { height: 100%; }\n" +
            "    body {\n" +
            "      font-family: 'DM Sans', sans-serif;\n" +
            "      background: var(--cream);\n" +
            "      color: var(--navy);\n" +
            "      display: flex;\n" +
            "      flex-direction: column;\n" +
            "      height: 100vh;\n" +
            "      overflow: hidden;\n" +
            "    }\n" +
            "    /* NAV */\n" +
            "    nav {\n" +
            "      display: flex;\n" +
            "      align-items: center;\n" +
            "      justify-content: space-between;\n" +
            "      padding: 0 2rem;\n" +
            "      height: 60px;\n" +
            "      background: var(--warm-white);\n" +
            "      border-bottom: 1px solid var(--sand);\n" +
            "      flex-shrink: 0;\n" +
            "    }\n" +
            "    .nav-brand {\n" +
            "      font-family: 'Playfair Display', serif;\n" +
            "      font-style: italic;\n" +
            "      font-size: 1.4rem;\n" +
            "      color: var(--navy);\n" +
            "      letter-spacing: -0.01em;\n" +
            "    }\n" +
            "    .nav-links { display: flex; gap: 1.5rem; align-items: center; }\n" +
            "    .nav-links a {\n" +
            "      font-size: 0.82rem;\n" +
            "      color: var(--text-muted);\n" +
            "      text-decoration: none;\n" +
            "      letter-spacing: 0.02em;\n" +
            "      transition: color 0.2s;\n" +
            "    }\n" +
            "    .nav-links a:hover { color: var(--navy); }\n" +
            "    .nav-cta {\n" +
            "      background: var(--navy);\n" +
            "      color: white !important;\n" +
            "      padding: 0.4rem 1rem;\n" +
            "      border-radius: 20px;\n" +
            "      font-size: 0.8rem !important;\n" +
            "      font-weight: 500;\n" +
            "    }\n" +
            "    .nav-cta:hover { background: var(--navy-light) !important; color: white !important; }\n" +
            "    /* MAIN LAYOUT */\n" +
            "    .main {\n" +
            "      display: flex;\n" +
            "      flex: 1;\n" +
            "      overflow: hidden;\n" +
            "    }\n" +
            "    /* HERO SIDEBAR */\n" +
            "    .hero {\n" +
            "      width: 320px;\n" +
            "      flex-shrink: 0;\n" +
            "      background: var(--navy);\n" +
            "      background-image: url('https://i.imgur.com/ffssKml.png');\n" +
            "      background-size: cover;\n" +
            "      background-position: center;\n" +
            "      position: relative;\n" +
            "      padding: 2.5rem 2rem;\n" +
            "      display: flex;\n" +
            "      flex-direction: column;\n" +
            "      gap: 1.5rem;\n" +
            "      overflow-y: auto;\n" +
            "    }\n" +
            "    .hero::before {\n" +
            "      content: '';\n" +
            "      position: absolute;\n" +
            "      inset: 0;\n" +
            "      background: rgba(27, 42, 74, 0.55);\n" +
            "      pointer-events: none;\n" +
            "    }\n" +
            "    .hero > * { position: relative; z-index: 1; }\n" +
            "    .hero-eyebrow {\n" +
            "      font-size: 0.72rem;\n" +
            "      font-weight: 500;\n" +
            "      letter-spacing: 0.12em;\n" +
            "      text-transform: uppercase;\n" +
            "      color: var(--blush);\n" +
            "    }\n" +
            "    .hero-title {\n" +
            "      font-family: 'Playfair Display', serif;\n" +
            "      font-size: 2rem;\n" +
            "      line-height: 1.2;\n" +
            "      color: white;\n" +
            "    }\n" +
            "    .hero-title em { font-style: italic; color: var(--blush); }\n" +
            "    .hero-desc {\n" +
            "      font-size: 0.88rem;\n" +
            "      line-height: 1.6;\n" +
            "      color: rgba(255,255,255,0.65);\n" +
            "    }\n" +
            "    .hero-badges { display: flex; flex-wrap: wrap; gap: 0.5rem; }\n" +
            "    .hero-badge {\n" +
            "      background: rgba(255,255,255,0.1);\n" +
            "      color: rgba(255,255,255,0.8);\n" +
            "      padding: 0.3rem 0.75rem;\n" +
            "      border-radius: 20px;\n" +
            "      font-size: 0.75rem;\n" +
            "      border: 1px solid rgba(255,255,255,0.15);\n" +
            "    }\n" +
            "    .hero-divider {\n" +
            "      height: 1px;\n" +
            "      background: rgba(255,255,255,0.1);\n" +
            "    }\n" +
            "    .hero-quick-actions { display: flex; flex-direction: column; gap: 0.5rem; }\n" +
            "    .hero-quick-label {\n" +
            "      font-size: 0.72rem;\n" +
            "      text-transform: uppercase;\n" +
            "      letter-spacing: 0.08em;\n" +
            "      color: rgba(255,255,255,0.4);\n" +
            "      margin-bottom: 0.25rem;\n" +
            "    }\n" +
            "    .quick-btn {\n" +
            "      background: rgba(255,255,255,0.06);\n" +
            "      border: 1px solid rgba(255,255,255,0.12);\n" +
            "      color: rgba(255,255,255,0.75);\n" +
            "      padding: 0.55rem 0.875rem;\n" +
            "      border-radius: 8px;\n" +
            "      font-size: 0.8rem;\n" +
            "      cursor: pointer;\n" +
            "      text-align: left;\n" +
            "      transition: all 0.15s;\n" +
            "      font-family: 'DM Sans', sans-serif;\n" +
            "    }\n" +
            "    .quick-btn:hover {\n" +
            "      background: rgba(255,255,255,0.12);\n" +
            "      color: white;\n" +
            "    }\n" +
            "    /* CHAT AREA */\n" +
            "    .chat-area {\n" +
            "      flex: 1;\n" +
            "      display: flex;\n" +
            "      flex-direction: column;\n" +
            "      overflow: hidden;\n" +
            "    }\n" +
            "    /* SOFIA HEADER */\n" +
            "    .sofia-header {\n" +
            "      padding: 1rem 1.5rem;\n" +
            "      background: var(--warm-white);\n" +
            "      border-bottom: 1px solid var(--sand);\n" +
            "      display: flex;\n" +
            "      align-items: center;\n" +
            "      gap: 0.75rem;\n" +
            "      flex-shrink: 0;\n" +
            "    }\n" +
            "    .sofia-avatar {\n" +
            "      width: 38px; height: 38px;\n" +
            "      background: var(--navy);\n" +
            "      border-radius: 50%;\n" +
            "      display: flex; align-items: center; justify-content: center;\n" +
            "      font-family: 'Playfair Display', serif;\n" +
            "      font-style: italic;\n" +
            "      font-size: 1rem;\n" +
            "      color: var(--blush);\n" +
            "      flex-shrink: 0;\n" +
            "    }\n" +
            "    .sofia-info { flex: 1; }\n" +
            "    .sofia-name {\n" +
            "      font-weight: 500;\n" +
            "      font-size: 0.95rem;\n" +
            "      color: var(--navy);\n" +
            "    }\n" +
            "    .sofia-title {\n" +
            "      font-size: 0.75rem;\n" +
            "      color: var(--text-muted);\n" +
            "    }\n" +
            "    .sofia-status {\n" +
            "      width: 8px; height: 8px;\n" +
            "      background: #4ade80;\n" +
            "      border-radius: 50%;\n" +
            "      box-shadow: 0 0 0 2px rgba(74,222,128,0.2);\n" +
            "    }\n" +
            "    /* MESSAGES */\n" +
            "    #messages {\n" +
            "      flex: 1;\n" +
            "      overflow-y: auto;\n" +
            "      padding: 1.5rem;\n" +
            "      display: flex;\n" +
            "      flex-direction: column;\n" +
            "      gap: 1rem;\n" +
            "      background: var(--cream);\n" +
            "    }\n" +
            "    #messages::-webkit-scrollbar { width: 4px; }\n" +
            "    #messages::-webkit-scrollbar-track { background: transparent; }\n" +
            "    #messages::-webkit-scrollbar-thumb { background: var(--sand); border-radius: 2px; }\n" +
            "    .msg { display: flex; gap: 0.625rem; max-width: 80%; }\n" +
            "    .msg.user { align-self: flex-end; flex-direction: row-reverse; }\n" +
            "    .msg.agent { align-self: flex-start; }\n" +
            "    .msg-avatar {\n" +
            "      width: 30px; height: 30px;\n" +
            "      background: var(--navy);\n" +
            "      border-radius: 50%;\n" +
            "      display: flex; align-items: center; justify-content: center;\n" +
            "      font-family: 'Playfair Display', serif;\n" +
            "      font-style: italic;\n" +
            "      font-size: 0.75rem;\n" +
            "      color: var(--blush);\n" +
            "      flex-shrink: 0;\n" +
            "      margin-top: 2px;\n" +
            "    }\n" +
            "    .msg-bubble {\n" +
            "      background: white;\n" +
            "      border: 1px solid var(--sand);\n" +
            "      border-radius: 16px;\n" +
            "      border-bottom-left-radius: 4px;\n" +
            "      padding: 0.75rem 1rem;\n" +
            "      font-size: 0.875rem;\n" +
            "      line-height: 1.6;\n" +
            "      color: var(--navy);\n" +
            "      box-shadow: 0 1px 3px rgba(0,0,0,0.04);\n" +
            "    }\n" +
            "    .msg.user .msg-bubble {\n" +
            "      background: var(--navy);\n" +
            "      color: white;\n" +
            "      border: none;\n" +
            "      border-bottom-right-radius: 4px;\n" +
            "      border-bottom-left-radius: 16px;\n" +
            "    }\n" +
            "    .msg-bubble ol, .msg-bubble ul { padding-left: 1.25rem; margin: 0.4rem 0; }\n" +
            "    .msg-bubble li { margin-bottom: 0.25rem; }\n" +
            "    .msg-bubble strong { font-weight: 600; }\n" +
            "    .msg-bubble a { color: var(--rose); text-decoration: underline; }\n" +
            "    .msg.user .msg-bubble a { color: var(--blush); }\n" +
            "    /* TYPING INDICATOR */\n" +
            "    .typing { display: flex; gap: 4px; padding: 0.5rem 0.25rem; align-items: center; }\n" +
            "    .typing span {\n" +
            "      width: 6px; height: 6px;\n" +
            "      background: var(--rose);\n" +
            "      border-radius: 50%;\n" +
            "      animation: bounce 1.2s infinite;\n" +
            "    }\n" +
            "    .typing span:nth-child(2) { animation-delay: 0.2s; }\n" +
            "    .typing span:nth-child(3) { animation-delay: 0.4s; }\n" +
            "    @keyframes bounce {\n" +
            "      0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }\n" +
            "      30% { transform: translateY(-5px); opacity: 1; }\n" +
            "    }\n" +
            "    /* INPUT */\n" +
            "    .input-area {\n" +
            "      padding: 1rem 1.5rem;\n" +
            "      background: var(--warm-white);\n" +
            "      border-top: 1px solid var(--sand);\n" +
            "      display: flex;\n" +
            "      gap: 0.75rem;\n" +
            "      align-items: flex-end;\n" +
            "      flex-shrink: 0;\n" +
            "    }\n" +
            "    #input {\n" +
            "      flex: 1;\n" +
            "      border: 1.5px solid var(--sand);\n" +
            "      border-radius: 12px;\n" +
            "      padding: 0.75rem 1rem;\n" +
            "      font-size: 0.875rem;\n" +
            "      font-family: 'DM Sans', sans-serif;\n" +
            "      background: var(--cream);\n" +
            "      color: var(--navy);\n" +
            "      resize: none;\n" +
            "      outline: none;\n" +
            "      transition: border-color 0.2s;\n" +
            "      min-height: 44px;\n" +
            "      max-height: 120px;\n" +
            "    }\n" +
            "    #input:focus { border-color: var(--rose); }\n" +
            "    #input::placeholder { color: var(--text-muted); }\n" +
            "    #send {\n" +
            "      background: var(--rose);\n" +
            "      color: white;\n" +
            "      border: none;\n" +
            "      border-radius: 12px;\n" +
            "      width: 44px; height: 44px;\n" +
            "      cursor: pointer;\n" +
            "      display: flex; align-items: center; justify-content: center;\n" +
            "      transition: background 0.2s, transform 0.1s;\n" +
            "      flex-shrink: 0;\n" +
            "    }\n" +
            "    #send:hover { background: var(--deep-rose); transform: scale(1.05); }\n" +
            "    #send:active { transform: scale(0.95); }\n" +
            "    /* TRUST STRIP */\n" +
            "    .trust-strip {\n" +
            "      padding: 0.5rem 1.5rem;\n" +
            "      background: var(--warm-white);\n" +
            "      border-top: 1px solid var(--sand-light);\n" +
            "      display: flex;\n" +
            "      gap: 1.25rem;\n" +
            "      justify-content: center;\n" +
            "      flex-wrap: wrap;\n" +
            "      flex-shrink: 0;\n" +
            "    }\n" +
            "    .trust-item {\n" +
            "      display: flex;\n" +
            "      align-items: center;\n" +
            "      gap: 0.3rem;\n" +
            "      font-size: 0.72rem;\n" +
            "      color: var(--text-muted);\n" +
            "    }\n" +
            "    .trust-item svg { color: var(--rose); }\n" +
            "    /* FOOTER LINKS */\n" +
            "    .footer-links {\n" +
            "      padding: 0.4rem 1.5rem;\n" +
            "      background: var(--warm-white);\n" +
            "      display: flex;\n" +
            "      gap: 1rem;\n" +
            "      justify-content: center;\n" +
            "      flex-wrap: wrap;\n" +
            "      flex-shrink: 0;\n" +
            "    }\n" +
            "    .footer-links a {\n" +
            "      font-size: 0.72rem;\n" +
            "      color: var(--text-muted);\n" +
            "      text-decoration: none;\n" +
            "    }\n" +
            "    .footer-links a:hover { color: var(--rose); }\n" +
            "    @media (max-width: 768px) {\n" +
            "      .hero { display: none; }\n" +
            "    }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "  <nav>\n" +
            "    <span class=\"nav-brand\">StyleDesk</span>\n" +
            "    <div class=\"nav-links\">\n" +
            "      <a href=\"/agentfacts.json\">AgentFacts</a>\n" +
            "      <a href=\"/.well-known/agent-card.json\">Agent Card</a>\n" +
            "      <a href=\"/skill.md\">skill.md</a>\n" +
            "      <a href=\"/health\">Health</a>\n" +
            "      <a href=\"/request-access\" class=\"nav-cta\">Get API Key →</a>\n" +
            "    </div>\n" +
            "  </nav>\n" +
            "  <div class=\"main\">\n" +
            "    <aside class=\"hero\">\n" +
            "      <div>\n" +
            "        <p class=\"hero-eyebrow\">Personal Stylist · Fashion AI</p>\n" +
            "      </div>\n" +
            "      <div>\n" +
            "        <h1 class=\"hero-title\">Your wardrobe deserves <em>Sofia</em></h1>\n" +
            "      </div>\n" +
            "      <p class=\"hero-desc\">Sofia remembers your style, finds real products with buy links, and negotiates the best deals — all in one conversation.</p>\n" +
            "      <div class=\"hero-badges\">\n" +
            "        <span class=\"hero-badge\">Multi-turn memory</span>\n" +
            "        <span class=\"hero-badge\">Negotiation tactics</span>\n" +
            "        <span class=\"hero-badge\">Price comparison</span>\n" +
            "        <span class=\"hero-badge\">Live product search</span>\n" +
            "      </div>\n" +
            "      <div class=\"hero-divider\"></div>\n" +
            "      <div class=\"hero-quick-actions\">\n" +
            "        <p class=\"hero-quick-label\">Try asking</p>\n" +
            "        <button class=\"quick-btn\" onclick=\"setInput('I need a winter gala outfit under $300')\">Winter gala outfit under $300</button>\n" +
            "        <button class=\"quick-btn\" onclick=\"setInput('Find me the best deal on a cashmere coat')\">Best deal on a cashmere coat</button>\n" +
            "        <button class=\"quick-btn\" onclick=\"setInput('How do I negotiate a discount at a boutique?')\">Negotiate a boutique discount</button>\n" +
            "        <button class=\"quick-btn\" onclick=\"setInput('What are the top fashion trends this season?')\">Top trends this season</button>\n" +
            "        <button class=\"quick-btn\" onclick=\"setInput('Build me a capsule wardrobe for work')\">Capsule wardrobe for work</button>\n" +
            "      </div>\n" +
            "    </aside>\n" +
            "    <div class=\"chat-area\">\n" +
            "      <div class=\"sofia-header\">\n" +
            "        <div class=\"sofia-avatar\">S</div>\n" +
            "        <div class=\"sofia-info\">\n" +
            "          <div class=\"sofia-name\">Sofia</div>\n" +
            "          <div class=\"sofia-title\">Personal Stylist · StyleDesk</div>\n" +
            "        </div>\n" +
            "        <div class=\"sofia-status\"></div>\n" +
            "      </div>\n" +
            "      <div id=\"messages\"></div>\n" +
            "      <div class=\"input-area\">\n" +
            "        <textarea id=\"input\" rows=\"1\" placeholder=\"Ask Sofia about style, outfits, deals...\"></textarea>\n" +
            "        <button id=\"send\">\n" +
            "          <svg width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\">\n" +
            "            <line x1=\"22\" y1=\"2\" x2=\"11\" y2=\"13\"></line>\n" +
            "            <polygon points=\"22 2 15 22 11 13 2 9 22 2\"></polygon>\n" +
            "          </svg>\n" +
            "        </button>\n" +
            "      </div>\n" +
            "      <div class=\"trust-strip\">\n" +
            "        <span class=\"trust-item\"><svg width=\"12\" height=\"12\" fill=\"currentColor\" viewBox=\"0 0 16 16\"><path d=\"M13.854 3.646a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708 0l-3.5-3.5a.5.5 0 1 1 .708-.708L6.5 10.293l6.646-6.647a.5.5 0 0 1 .708 0z\"/></svg> Real product links</span>\n" +
            "        <span class=\"trust-item\"><svg width=\"12\" height=\"12\" fill=\"currentColor\" viewBox=\"0 0 16 16\"><path d=\"M13.854 3.646a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708 0l-3.5-3.5a.5.5 0 1 1 .708-.708L6.5 10.293l6.646-6.647a.5.5 0 0 1 .708 0z\"/></svg> Remembers your style</span>\n" +
            "        <span class=\"trust-item\"><svg width=\"12\" height=\"12\" fill=\"currentColor\" viewBox=\"0 0 16 16\"><path d=\"M13.854 3.646a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708 0l-3.5-3.5a.5.5 0 1 1 .708-.708L6.5 10.293l6.646-6.647a.5.5 0 0 1 .708 0z\"/></svg> Negotiation tactics</span>\n" +
            "        <span class=\"trust-item\"><svg width=\"12\" height=\"12\" fill=\"currentColor\" viewBox=\"0 0 16 16\"><path d=\"M13.854 3.646a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708 0l-3.5-3.5a.5.5 0 1 1 .708-.708L6.5 10.293l6.646-6.647a.5.5 0 0 1 .708 0z\"/></svg> Google Shopping + Amazon</span>\n" +
            "      </div>\n" +
            "      <div class=\"footer-links\">\n" +
            "        <a href=\"https://gocodeminder.com\">gocodeminder.com</a>\n" +
            "        <a href=\"/request-access\">API Access →</a>\n" +
            "        <a href=\"/.well-known/agent-card.json\">Agent Card</a>\n" +
            "        <a href=\"/agentfacts.json\">AgentFacts</a>\n" +
            "        <a href=\"/skill.md\">skill.md</a>\n" +
            "        <a href=\"/health\">Health</a>\n" +
            "      </div>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "  <script>\n" +
            "    const userId = 'sofia-web-' + Math.random().toString(36).slice(2, 9);\n" +
            "    let apiKey = null;\n" +
            "\n" +
            "    async function getApiKey() {\n" +
            "      const r = await fetch('/request-access', {\n" +
            "        method: 'POST',\n" +
            "        headers: { 'Content-Type': 'application/json' },\n" +
            "        body: JSON.stringify({ principal: userId, name: 'Web User', reason: 'chat' })\n" +
            "      });\n" +
            "      const d = await r.json();\n" +
            "      return d.apiKey;\n" +
            "    }\n" +
            "\n" +
            "    function renderMarkdown(text) {\n" +
            "      text = text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');\n" +
            "      text = text.replace(/\\*\\*(.*?)\\*\\*/g, '<strong>$1</strong>');\n" +
            "      text = text.replace(/\\[([^\\]]+)\\]\\(([^)]+)\\)/g, '<a href=\"$2\" target=\"_blank\" rel=\"noopener\">$1</a>');\n" +
            "      text = text.replace(/(?:^|\\n)(\\d+)\\. (.+)/g, '\\n<li>$2</li>');\n" +
            "      text = text.replace(/((?:\\n<li>.*<\\/li>)+)/g, '<ol>$1</ol>');\n" +
            "      text = text.replace(/(?:^|\\n)[\\*\\-]\\s+(.+)/g, '\\n<li>$1</li>');\n" +
            "      text = text.replace(/((?:\\n<li>.*<\\/li>)+)/g, '<ul>$1</ul>');\n" +
            "      text = text.replace(/\\n/g, '<br>');\n" +
            "      return text;\n" +
            "    }\n" +
            "\n" +
            "    function addMessage(text, role) {\n" +
            "      const msgs = document.getElementById('messages');\n" +
            "      const div = document.createElement('div');\n" +
            "      div.className = 'msg ' + role;\n" +
            "      const rendered = role === 'agent' ? renderMarkdown(text) : text;\n" +
            "      if (role === 'agent') {\n" +
            "        div.innerHTML = '<div class=\"msg-avatar\">S</div><div class=\"msg-bubble\">' + rendered + '</div>';\n" +
            "      } else {\n" +
            "        div.innerHTML = '<div class=\"msg-bubble\">' + rendered + '</div>';\n" +
            "      }\n" +
            "      msgs.appendChild(div);\n" +
            "      msgs.scrollTop = msgs.scrollHeight;\n" +
            "    }\n" +
            "\n" +
            "    function addTyping() {\n" +
            "      const msgs = document.getElementById('messages');\n" +
            "      const div = document.createElement('div');\n" +
            "      div.className = 'msg agent'; div.id = 'typing-indicator';\n" +
            "      div.innerHTML = '<div class=\"msg-avatar\">S</div><div class=\"msg-bubble\"><div class=\"typing\"><span></span><span></span><span></span></div></div>';\n" +
            "      msgs.appendChild(div);\n" +
            "      msgs.scrollTop = msgs.scrollHeight;\n" +
            "    }\n" +
            "\n" +
            "    function removeTyping() {\n" +
            "      const t = document.getElementById('typing-indicator');\n" +
            "      if (t) t.remove();\n" +
            "    }\n" +
            "\n" +
            "    function setInput(text) {\n" +
            "      document.getElementById('input').value = text;\n" +
            "      document.getElementById('input').focus();\n" +
            "    }\n" +
            "\n" +
            "    async function sendMessage() {\n" +
            "      const input = document.getElementById('input');\n" +
            "      const text = input.value.trim();\n" +
            "      if (!text) return;\n" +
            "      input.value = '';\n" +
            "      input.style.height = 'auto';\n" +
            "      addMessage(text, 'user');\n" +
            "      addTyping();\n" +
            "      try {\n" +
            "        if (!apiKey) apiKey = await getApiKey();\n" +
            "        const r = await fetch('/chat', {\n" +
            "          method: 'POST',\n" +
            "          headers: { 'Content-Type': 'application/json', 'X-API-Key': apiKey },\n" +
            "          body: JSON.stringify({ userId, message: text })\n" +
            "        });\n" +
            "        const d = await r.json();\n" +
            "        removeTyping();\n" +
            "        addMessage(d.sofia || d.error || 'Something went wrong', 'agent');\n" +
            "      } catch (e) {\n" +
            "        removeTyping();\n" +
            "        addMessage('Error: ' + e.message, 'agent');\n" +
            "        console.error('Chat error:', e);\n" +
            "      }\n" +
            "    }\n" +
            "\n" +
            "    document.getElementById('send').addEventListener('click', sendMessage);\n" +
            "    document.getElementById('input').addEventListener('keydown', e => {\n" +
            "      if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); }\n" +
            "    });\n" +
            "    document.getElementById('input').addEventListener('input', function() {\n" +
            "      this.style.height = 'auto';\n" +
            "      this.style.height = Math.min(this.scrollHeight, 120) + 'px';\n" +
            "    });\n" +
            "\n" +
            "    // Welcome message\n" +
            "    addMessage(\"Hi! I'm Sofia, your personal stylist. I can find real outfits with buy links, compare prices across Google Shopping and Amazon, and give you negotiation tactics for the best deals. What can I help you with today?\", 'agent');\n" +
            "  </script>\n" +
            "</body>\n" +
            "</html>\n";
        return Response.ok(html).build();
    }

    /** Favicon */
    @GET
    @Path("favicon.svg")
    @Produces("image/svg+xml")
    public Response faviconSvg() {
        String svg =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<svg width=\"32\" height=\"32\" viewBox=\"0 0 32 32\" xmlns=\"http://www.w3.org/2000/svg\">" +
            "<circle cx=\"16\" cy=\"16\" r=\"16\" fill=\"#1b2a4a\"/>" +
            "<text x=\"16\" y=\"22\" text-anchor=\"middle\" font-family=\"Georgia,serif\" font-style=\"italic\" font-size=\"18\" fill=\"#e8c4b8\">S</text>" +
            "</svg>";
        return Response.ok(svg).build();
    }

    /**
     * Conversational chat endpoint — maintains memory per userId.
     * Each userId gets their own session so Sofia remembers context.
     */
    @POST
    @Path("chat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response chat(Map<String, Object> body) {
        try {
            String userId = (String) body.getOrDefault("userId", "anonymous");
            String message = (String) body.get("message");

            if (message == null || message.isBlank()) {
                return Response.status(400).entity(Map.of(
                    "error", "Missing 'message' field",
                    "example", Map.of(
                        "userId", "user123",
                        "message", "Hi Sofia! I need a winter gala outfit under $300"
                    )
                )).build();
            }

            // Get or create session per user — this is what enables memory
            Session session = sessions.computeIfAbsent(userId, id ->
                runner.sessionService()
                    .createSession(runner.appName(), id)
                    .blockingGet()
            );

            Content userMsg = Content.fromParts(Part.fromText(message));
            RunConfig runConfig = RunConfig.builder().build();

            Flowable<Event> events = runner.runAsync(
                session.userId(), session.id(), userMsg, runConfig
            );

            StringBuilder response = new StringBuilder();
            events.blockingForEach(event -> {
                if (event.finalResponse()) {
                    event.content().ifPresent(c ->
                        c.parts().ifPresent(parts ->
                            parts.forEach(p ->
                                p.text().ifPresent(response::append)
                            )
                        )
                    );
                }
            });

            return Response.ok(Map.of(
                "userId", userId,
                "sofia", response.toString(),
                "sessionActive", true,
                "tip", "Keep using the same userId to maintain conversation memory"
            )).build();

        } catch (Exception e) {
            return Response.serverError().entity(Map.of(
                "error", e.getMessage()
            )).build();
        }
    }

    /** A2A endpoint — for other agents to call StyleDesk */
    @POST
    @Path("a2a")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleA2A(Map<String, Object> request) {
        try {
            String userId = "a2a-" + request.getOrDefault("id", "agent");
            Session session = sessions.computeIfAbsent(userId, id ->
                runner.sessionService()
                    .createSession(runner.appName(), id)
                    .blockingGet()
            );

            String userText = extractMessage(request);
            Content userMsg = Content.fromParts(Part.fromText(userText));
            RunConfig runConfig = RunConfig.builder().build();

            Flowable<Event> events = runner.runAsync(
                session.userId(), session.id(), userMsg, runConfig
            );

            StringBuilder response = new StringBuilder();
            events.blockingForEach(event -> {
                if (event.finalResponse()) {
                    event.content().ifPresent(c ->
                        c.parts().ifPresent(parts ->
                            parts.forEach(p ->
                                p.text().ifPresent(response::append)
                            )
                        )
                    );
                }
            });

            return Response.ok(Map.of(
                "jsonrpc", "2.0",
                "id", request.getOrDefault("id", "1"),
                "result", Map.of(
                    "status", Map.of("state", "completed"),
                    "artifacts", java.util.List.of(
                        Map.of("parts", java.util.List.of(
                            Map.of("type", "text", "text", response.toString())
                        ))
                    )
                )
            )).build();

        } catch (Exception e) {
            return Response.serverError().entity(Map.of(
                "jsonrpc", "2.0",
                "error", Map.of("code", -32603, "message", e.getMessage())
            )).build();
        }
    }

    /** Agent card */
    @GET
    @Path(".well-known/agent-card.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response agentCard() {
        return Response.ok(Map.of(
            "name", "styledesk-agent",
            "description", "Sofia — a conversational personal stylist with negotiation skills. Remembers your preferences and helps you find the best fashion deals.",
            "url", BASE_URL,
            "version", "1.0.0",
            "capabilities", Map.of("streaming", false, "memory", true),
            "authentication", Map.of(
                "type", "api_key",
                "header", "X-API-Key",
                "howToGet", "POST /request-access"
            ),
            "endpoints", Map.of(
                "chat", "POST /chat — conversational with memory",
                "a2a", "POST /a2a — A2A JSON-RPC",
                "requestAccess", "POST /request-access"
            ),
            "connectedAgents", java.util.List.of(
                "fashion-agent @ https://fashion-agent-246551819686.us-central1.run.app"
            )
        )).build();
    }

    /** AgentFacts */
    @GET
    @Path("agentfacts.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response agentFacts() {
        return Response.ok(Map.of(
            "@context", "https://spec.projectnanda.org/agentfacts/v1.2.jsonld",
            "id", "did:nanda:styledesk-agent-001",
            "handle", "@styledesk-agent",
            "endpoint", BASE_URL + "/a2a",
            "capabilities", java.util.List.of(
                "urn:nanda:cap:conversational-styling:v1",
                "urn:nanda:cap:price-negotiation:v1",
                "urn:nanda:cap:price-comparison:v1",
                "urn:nanda:cap:preference-memory:v1"
            ),
            "auth", Map.of(
                "method", "api_key",
                "header", "X-API-Key",
                "howToGet", BASE_URL + "/request-access"
            ),
            "meta", Map.of(
                "description", "Conversational fashion stylist with negotiation capabilities",
                "builtWith", "Google ADK for Java",
                "collaborates", "fashion-agent"
            )
        )).build();
    }

    /** skill.md */
    @GET
    @Path("skill.md")
    @Produces("text/markdown")
    public Response skillMd() {
        return Response.ok(
            "---\n" +
            "name: styledesk-agent\n" +
            "description: Sofia — conversational personal stylist with negotiation skills and fashion memory.\n" +
            "metadata:\n" +
            "  endpoint: " + BASE_URL + "/chat\n" +
            "  auth: api_key (X-API-Key)\n" +
            "  get_access: POST " + BASE_URL + "/request-access\n" +
            "---\n\n" +
            "## StyleDesk Agent — Meet Sofia\n\n" +
            "A conversational personal stylist who remembers your preferences and negotiates deals.\n\n" +
            "### Unique capabilities\n" +
            "- **Memory** — remembers style preferences across conversations\n" +
            "- **Negotiation** — tactics for boutiques, online, markets\n" +
            "- **Price comparison** — finds best deals across retailers\n" +
            "- **FashionAgent connected** — calls specialist fashion agent for outfit advice\n\n" +
            "### Chat endpoint (with memory)\n" +
            "```bash\n" +
            "curl -X POST " + BASE_URL + "/chat \\\n" +
            "  -H 'X-API-Key: YOUR_KEY' \\\n" +
            "  -H 'Content-Type: application/json' \\\n" +
            "  -d '{\"userId\":\"user123\",\"message\":\"Hi Sofia! I need a winter gala outfit\"}'\n" +
            "```\n"
        ).build();
    }

    /** Health */
    @GET
    @Path("health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok(Map.of(
            "status", "UP",
            "agent", "styledesk-agent",
            "persona", "Sofia",
            "connectedTo", "fashion-agent"
        )).build();
    }

    /** Access request — auto grants API key */
    @POST
    @Path("request-access")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestAccess(Map<String, String> body) {
        String principal = body.getOrDefault("principal", "anonymous");
        String name = body.getOrDefault("name", "unknown");
        String apiKey = "sk-styledesk-" + java.util.UUID.randomUUID()
            .toString().replace("-", "").substring(0, 24);
        return Response.ok(Map.of(
            "status", "granted",
            "principal", principal,
            "apiKey", apiKey,
            "message", "Welcome! I'm Sofia, your personal stylist. Use your API key to start chatting.",
            "chatEndpoint", BASE_URL + "/chat",
            "example", Map.of(
                "userId", principal,
                "message", "Hi Sofia! I need help finding a formal winter outfit under $300"
            )
        )).build();
    }

    @SuppressWarnings("unchecked")
    private String extractMessage(Map<String, Object> request) {
        try {
            Map<String, Object> params = (Map<String, Object>) request.get("params");
            if (params != null) {
                Map<String, Object> message = (Map<String, Object>) params.get("message");
                if (message != null) {
                    var parts = (java.util.List<Map<String, Object>>) message.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
        } catch (Exception ignored) {}
        return "Hello Sofia, what can you help me with today?";
    }
}