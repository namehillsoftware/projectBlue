Engine ->
    - PlayableFile
    - Preparation ->
        - Queue ->
            - PreparedPlayableFileQueue
        - PlayableFilePreparationSource
        - PreparedPlayableFile
Playlist ->
    -> Play: PlayingPlaylist -> Future state
    -> Pause: PausedPlaylist -> Future state
    -> Resume: PlayingPlaylist -> Future state
    -> ChangePosition: 
    -> Add
    -> Remove
    -> Insert



Within a playback engine, two items need synchronization, but not necessarily with each other:

- The "player"
    - The "player" actively progresses through media and through a playlist
    - Do not want two different players active at once.
    - The player depends on the now playing data to start, to save current progress, etc.
- "Now Playing data"
    - The now playing data contains the list of media to play, and the last stored playback progress.

*Starting a new playlist*

```plantuml
@startuml

partition Engine {
    (*) --> "Start playlist"
    --> "Save playlist to state"
}

partition "Start Player" {
    --> "Start playlist from state"
    --> "Get playlist from state"
    --> "Initialize playback queue"
    --> "Create playlist player"
}

partition Player {
    --> "Play playlist"
}

@enduml
```

*Restoring a playlist*

```plantuml
@startuml

partition Engine {
    (*) --> "Resume"
}

partition "Start Player" {
    --> "Start playlist from state"
    --> "Get playlist from state"
    --> "Initialize playback queue"
    --> "promisePlayedPlaylist(..)"
}

partition Player {
    --> "Play playlist"
}

@enduml
```
