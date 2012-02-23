/* //device/samples/SampleCode/src/com/android/samples/app/RemoteServiceInterface.java
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**	 http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package org.yammp;

import android.graphics.Bitmap;
import android.net.Uri;

interface IMusicPlaybackService {

	void reloadLyrics();
	void refreshLyrics();
	void openFile(String path);
	void open(in long [] list, int position);
	int getQueuePosition();
	boolean isPlaying();
	void stop();
	void pause();
	void play();
	void prev();
	void next();
	void cycleRepeat();
	void toggleShuffle();
	long duration();
	long position();
	long seek(long pos);
	String getTrackName();
	String getMediaPath();
	String getAlbumName();
	long getAlbumId();
	String getArtistName();
	long getArtistId();
	void enqueue(in long [] list, int action);
	long [] getQueue();
	void moveQueueItem(int from, int to);
	void setQueuePosition(int index);
	void setQueueId(long id);
	String getPath();
	long getAudioId();
	Bitmap getAlbumArt();
	Uri getArtworkUri();
	String [] getLyrics();
	int getLyricsStatus();
	int getCurrentLyricsId();
	long getPositionByLyricsId(int id);
	void setShuffleMode(int shufflemode);
	int getShuffleMode();
	int removeTracks(int first, int last);
	int removeTrack(long id);
	void setRepeatMode(int repeatmode);
	int getRepeatMode();
	int getMediaMountedCount();
	int getAudioSessionId();
	void startSleepTimer(long millisecond, boolean gentle);
	void stopSleepTimer();
	long getSleepTimerRemained();
	void reloadSettings();
	void reloadEqualizer();
	void toggleFavorite();
	void addToFavorites(long id);
	void removeFromFavorites(long id);
	boolean isFavorite(long id);

}