/*
 * Copyright 2015-2018 Jason Winning
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.hypernomicon.util;

import static org.hypernomicon.util.Util.*;

import org.hypernomicon.model.Exceptions.*;

import java.io.IOException;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;

public class AsyncHttpClient
{
  @FunctionalInterface public interface ExHandler     { public void handle(Exception e); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public class RequestThread extends Thread
  {
    private ResponseHandler<? extends Boolean> responseHandler;
    private ExHandler failHndlr;
    
    public RequestThread(ResponseHandler<? extends Boolean> responseHandler, ExHandler failHndlr)
    {
      super();
      
      setDaemon(true);
      
      this.responseHandler = responseHandler;
      this.failHndlr = failHndlr;
      
      start();
    }
    
    @Override public void run()
    {
      cancelledByUser = false;
      
      try (CloseableHttpClient httpclient = getHTTPClient())
      {
        httpclient.execute(request, responseHandler);        
      } 
      catch (IOException e)
      {
        if (cancelledByUser)
          runInFXThread(() -> failHndlr.handle(new TerminateTaskException()));
        else
          runInFXThread(() -> failHndlr.handle(e));
      }

      stopped = true;
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private HttpUriRequest request;
  private boolean stopped = true, cancelledByUser = false;
  private RequestThread requestThread;
  
  public boolean wasCancelledByUser() { return cancelledByUser; }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void doRequest(HttpUriRequest request, ResponseHandler<? extends Boolean> responseHandler, ExHandler failHndlr)
  {
    stop();
    
    this.request = request;
    requestThread = new RequestThread(responseHandler, failHndlr);
    stopped = false;
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public boolean isRunning()
  { 
    if (stopped == true) return false;
    return requestThread == null ? false : requestThread.isAlive(); 
  } 

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------
  
  public boolean stop()
  {
    boolean wasRunning = isRunning();
    
    if (requestThread != null)
      if (requestThread.isAlive())
      {
        if (request != null)
        {
          cancelledByUser = true;
          request.abort();          
        }
  
        try { requestThread.join(); } catch (InterruptedException e) { noOp(); }
        
        request = null;
      }
          
    requestThread = null;
    
    if (stopped == false)
      stopped = true;
    
    return wasRunning;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}