/*
 * 
 * Copyright 2014 Jules White
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
package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {

	/**
	 * You will need to create one or more Spring controllers to fulfill the
	 * requirements of the assignment. If you use this file, please rename it
	 * to something other than "AnEmptyController"
	 * 
	 * 
		 ________  ________  ________  ________          ___       ___  ___  ________  ___  __       
		|\   ____\|\   __  \|\   __  \|\   ___ \        |\  \     |\  \|\  \|\   ____\|\  \|\  \     
		\ \  \___|\ \  \|\  \ \  \|\  \ \  \_|\ \       \ \  \    \ \  \\\  \ \  \___|\ \  \/  /|_   
		 \ \  \  __\ \  \\\  \ \  \\\  \ \  \ \\ \       \ \  \    \ \  \\\  \ \  \    \ \   ___  \  
		  \ \  \|\  \ \  \\\  \ \  \\\  \ \  \_\\ \       \ \  \____\ \  \\\  \ \  \____\ \  \\ \  \ 
		   \ \_______\ \_______\ \_______\ \_______\       \ \_______\ \_______\ \_______\ \__\\ \__\
		    \|_______|\|_______|\|_______|\|_______|        \|_______|\|_______|\|_______|\|__| \|__|
                                                                                                                                                                                                                                                                        
	 * 
	 */
	
	private static final AtomicLong currentId = new AtomicLong(0L);
	private Map<Long,Video> videos = new HashMap<Long, Video>();
	private VideoFileManager videoDataMgr;

	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return videos.values();
	}

	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		Video video = Video.create().withContentType(v.getContentType())
				.withDuration(v.getDuration()).withSubject(v.getSubject())
				.withTitle(v.getTitle()).build();
		
		return save(video);
	}	
	
	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable (VideoSvcApi.ID_PARAMETER) long id, 
			@RequestParam (VideoSvcApi.DATA_PARAMETER) MultipartFile videoData,
			HttpServletResponse response) throws IOException {
		videoDataMgr = VideoFileManager.get();
		if (videos.containsKey(id)) {
			Video v = videos.get(id);
			saveSomeVideo(v, videoData);
		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}

		return new VideoStatus(VideoStatus.VideoState.READY);
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.GET)
	public void getData(
			@PathVariable(VideoSvcApi.ID_PARAMETER) long id, 
			HttpServletResponse response) throws IOException {
		if (videos.containsKey(id)) {
			Video v = videos.get(id);
			if (videoDataMgr.hasVideoData(v)) {
				serveSomeVideo(v, response);
				return;
			}
		}
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);		
	}
	
  	private Video save(Video entity) {
		checkAndSetId(entity);
		entity.setDataUrl(getDataUrl(entity.getId()));
		videos.put(entity.getId(), entity);
		return entity;
	}

	private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
		}
	}

    private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

 	private String getUrlBaseForLocalServer() {
	   HttpServletRequest request = 
	       ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	   String base = 
	      "http://"+request.getServerName() 
	      + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
	   return base;
	}

    // You would need some Controller method to call this...
  	private void saveSomeVideo(Video v, MultipartFile videoData) throws IOException {
  	     videoDataMgr.saveVideoData(v, videoData.getInputStream());
  	}
  	
  	private void serveSomeVideo(Video v, HttpServletResponse response) throws IOException {
  	     // Of course, you would need to send some headers, etc. to the
  	     // client too!
  	     //  ...
  	     videoDataMgr.copyVideoData(v, response.getOutputStream());
  	}

}
