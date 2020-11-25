/**
 * Copyright (C) 2014  Universidade de Aveiro, DETI/IEETA, Bioinformatics Group - http://bioinformatics.ua.pt/
 *
 * This file is part of Dicoogle/dicoogle.
 *
 * Dicoogle/dicoogle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dicoogle/dicoogle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dicoogle.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ua.dicoogle.server.web.servlets.management;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONObject;

import pt.ua.dicoogle.core.settings.ServerSettingsManager;
import pt.ua.dicoogle.sdk.settings.server.ServerSettings;
import pt.ua.dicoogle.sdk.settings.server.ServerSettingsReader;
import pt.ua.dicoogle.server.ControlServices;

/** Servlet for reading and writing DICOM service configurations.
 * Modifying the "running" setting will trigger a start or a stop on the actual service.
 *
 * At the moment, applying settings to PLUGIN-type services is not implemented, resulting in a no-op.
 * 
 * @author Frederico Silva <fredericosilva@ua.pt>
 */
public class ServicesServlet extends HttpServlet {
	
	public final static int STORAGE = 0;
	public final static int PLUGIN = 1;
	public final static int QUERY = 2;
	
	private final int mType;
	public ServicesServlet(int type) {
		if (type < 0 || type > 2) {
			throw new IllegalArgumentException("Bad service type, must be 0, 1 or 2");
		}
		mType = type;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		final ControlServices controlServices = ControlServices.getInstance();
		final ServerSettings serverSettings = ServerSettingsManager.getSettings();

		boolean isRunning = false; 
		int port = -1;
        boolean autostart = false;
		ServerSettingsReader.ServiceBase base;
		switch (mType) {
		case STORAGE:
			base = serverSettings.getDicomServicesSettings().getStorageSettings();
			isRunning = controlServices.storageIsRunning();
			port = base.getPort();
			autostart = base.isAutostart();
			break;
		case QUERY:
			base = serverSettings.getDicomServicesSettings().getQueryRetrieveSettings();
			isRunning = controlServices.queryRetrieveIsRunning();
			port = base.getPort();
			autostart = base.isAutostart();
			break;
		default:
			break;
		}
		
        JSONObject obj = new JSONObject();
        obj.element("isRunning", isRunning);
        obj.element("port", port);
        obj.element("autostart", autostart);
        
        resp.setContentType("application/json");
        resp.getWriter().print(obj.toString());
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

        JSONObject obj = new JSONObject();

		// gather settings to update
		boolean updateAutostart = false, updatePort = false, updateRunning = false;
		boolean autostart = false, running = false;
		int port = 0;

		String paramPort = req.getParameter("port");
		if (paramPort != null) {
			try {
				port = Integer.parseInt(paramPort);
				if (port <= 0 || port > 65535) {
					throw new NumberFormatException();
				}
				updatePort = true;
			} catch (NumberFormatException ex) {
				obj.element("success", false);
				obj.element("error", "Bad service port: must be a valid port number");
				reply(resp, 400, obj);
				return;
			}
		}

        String paramAutostart = req.getParameter("autostart");
        if (paramAutostart != null) {
            autostart = Boolean.parseBoolean(paramAutostart);
			updateAutostart = true;
        }
        
        String paramRunning = req.getParameter("running");
        if (paramRunning != null) {
			running = Boolean.parseBoolean(paramRunning);
			updateRunning = true;
        }

		final ControlServices controlServices = ControlServices.getInstance();
		final ServerSettings serverSettings = ServerSettingsManager.getSettings();

		// update auto-start
		if (updateAutostart) {
			switch (mType) {
				case STORAGE:
					serverSettings.getDicomServicesSettings().getStorageSettings().setAutostart(autostart);
					obj.element("autostart", autostart);
					break;
				case QUERY:
					serverSettings.getDicomServicesSettings().getQueryRetrieveSettings().setAutostart(autostart);
					obj.element("autostart", autostart);
					break;
			}
		}

		// update port
		if (updatePort) {
			switch (mType) {
				case STORAGE:
					serverSettings.getDicomServicesSettings().getStorageSettings().setPort(port);
					obj.element("port", port);
					break;
				case QUERY:
					serverSettings.getDicomServicesSettings().getQueryRetrieveSettings().setPort(port);
					obj.element("port", port);
					break;
			}
		}

		// update running
		if (updateRunning) {
			switch (mType) {
				case STORAGE:
					if (running) {
						controlServices.startStorage();
						obj.element("running", true);
					} else {
						controlServices.stopStorage();
						obj.element("running", false);
					}
					break;

				case QUERY:
					if (running) {
						controlServices.startQueryRetrieve();
						obj.element("running", true);

					} else {
						controlServices.stopQueryRetrieve();
						obj.element("running", false);
					}
					break;

				default:
					break;
			}
		}
		ServerSettingsManager.saveSettings();
		obj.element("success", true);
		reply(resp, 200, obj);
	}

	private static void reply(HttpServletResponse resp, int code, JSONObject object) throws IOException {
		resp.setContentType("application/json");
		resp.setStatus(code);
		resp.getWriter().print(object.toString());
	}

}
