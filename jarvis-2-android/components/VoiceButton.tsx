import React,{useEffect} from 'react';
import {Pressable,Text,StyleSheet} from 'react-native';
import {AudioModule,useAudioRecorder,RecordingPresets,useAudioRecorderState,setAudioModeAsync} from 'expo-audio';
import {transcribe} from '@/lib/api';
export function VoiceButton({onText}:{onText:(text:string)=>void}){const recorder=useAudioRecorder(RecordingPresets.HIGH_QUALITY);const state=useAudioRecorderState(recorder);useEffect(()=>{(async()=>{await AudioModule.requestRecordingPermissionsAsync();await setAudioModeAsync({playsInSilentMode:true,allowsRecording:true});})();},[]);const toggle=async()=>{if(state.isRecording){await recorder.stop();if(recorder.uri){try{const r=await transcribe(recorder.uri);if(r.text)onText(r.text);}catch{}}}else{await recorder.prepareToRecordAsync();recorder.record();}};return <Pressable onPress={toggle} style={[s.btn,state.isRecording&&s.active]}><Text style={s.txt}>{state.isRecording?'■ STOP':'● VOICE'}</Text></Pressable>}
const s=StyleSheet.create({btn:{backgroundColor:'#10172a',borderWidth:1,borderColor:'#1d2a45',borderRadius:13,padding:13,alignItems:'center'},active:{borderColor:'#ffc45a'},txt:{color:'#45d7ff',fontWeight:'800',fontSize:12}});
