<div align="center">
  <img width="100" height="100" alt="정체불명_logo1" src="https://github.com/user-attachments/assets/50eac4f2-f339-4943-bed8-f6f70e60d9fa" />
  <h3><b>정체불명: 길은 그대로, 경험은 새롭게!</b></h3>
  지루한 교통정체부터 설레는 여행길까지, 자동차에서 보내는 모든 순간을 모험으로 바꾸고 나만의 세계를 키워가는 차량 특화형 게임 앱 <!-- 한줄소개 --> 
  <br><br>
  
![Kotlin](https://img.shields.io/badge/Kotlin_2.2.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white) ![Pleos Connect](https://img.shields.io/badge/Pleos_Connect_2.1.0-002C5F?style=for-the-badge&logo=hyundai&logoColor=white)


🧑‍💻 [실행 방법](#실행-방법) ‣ [테스트 환경 및 설정](#테스트-환경-및-설정) ‣ [APK 다운로드](#apk-다운로드) ‣ [정체불명 앱 소개](#정체불명-앱-소개) ‣ [시연 영상](#시연영상) ‣ [숏폼](#숏폼) ‣ [만든 사람들](#team)👩‍💻





</div>

## 실행 방법

1. Pleos Connect 에뮬레이터를 실행하고 기어를 **P**로 설정합니다.
2. [APK 다운로드](#apk-다운로드) 후 아래 명령으로 설치합니다.

   ```bash
   adb install -r "apk/break가고장난8톤트럭_정체불명_release.apk"
   ```

3. 앱 목록에서 **정체불명**을 실행하고 제공되어 있는 계정으로 입장합니다.
   - 잠금 계정 PIN: `1234` (모든 계정 동일)

## 테스트 환경 및 설정

| 항목 | 환경 |
|---|---|
| 에뮬레이터 | Pleos Connect v2.1.0 |
| 운영체제 및 아키텍처 | Android 14 / API 34 / x86_64 |
| 화면 규격 | Connect-S-17(Default) |
| 네트워크 | 지도 표시 및 내비게이션 연동 시 인터넷 연결 필수 |

### 기어 설정

기어 설정 방법은 총 두 가지입니다.  운전 중 주의 분산을 방지하기 위해 기어 설정에 따라 특정 기능 활성화 여부가 달라지니, 꼭 확인해 주세요. 

| 기어 | 확인할 수 있는 기능 |
|---|---|
| P(주차) | 농장 / 집 꾸미기 / 사진 촬영 / 상점 / 친구 추가 / 도감 확인 / 보상 수령 등 |
| D(주행) | 홈 화면에서 주행 화면과 레이드 진행 확인 / 음성 안내 켜기 및 끄기|

**방법 1. Pleos 에뮬레이터의 Virtual Console에서 기어를 변경하여 테스트할 수 있습니다.**
```
Motion → Gear Selection
```


**방법 2. Python을 활용할 수 있습니다.**  

```python
python tools/gear.py D 
```

```python
python tools/gear.py P
```


## APK 다운로드

[정체불명 APK 다운로드](./apk/break가고장난8톤트럭_정체불명_release.apk)

## 정체불명 앱 소개

<div align="center">
  
### <기획 배경 및 핵심 기능>


### **1. 매일 같은 길을 오가는 운전자에게 교통정체는 반복되는 억겁의 시간 ⏳**
</div>


<img width="10242" height="2456" alt="drive_commute" src="https://github.com/user-attachments/assets/045101d9-ebd2-4710-a977-49c2caf4f509" />

정체불명은 운전자들이 겪는 정체로부터의 고통을 해소하고자 만들어졌습니다. **정체 구간을 보스**로 표현하고, 그 구간을 지나온 운전자의 소중한 시간을 보상과 수집으로 보답합니다. 주행 중에는 레이드가 자동으로 진행되고, 주차 후에는 보상을 확인하고 나만의 공간들을 가꿀 수 있습니다. **무료한 시간으로만 느껴졌던 출퇴근길이 수집과 성장의 발판이 되는 선물 같은 순간**이 됩니다. 안전운전을 한다면 뱃지와 추가 보상도 받을 수 있어요.
<br>
<br>
<br>


<div align="center">

### **2. 여행, 드라이브 등 나의 여정에 대한 소중한 기록 남기기 🎆**
</div>

<img width="3516" height="1320" alt="03-postcard-list-side" src="https://github.com/user-attachments/assets/4d3a93a6-d0e6-407e-ba1e-7d204d75cef7" />

<img width="4128" height="1760" alt="08-badge-list-side" src="https://github.com/user-attachments/assets/f9998364-774a-444e-a16e-09fd9e510523" />


자동차로 떠나는 여정들에 대해 특별한 기록을 남기고 싶지 않으신가요? 특정 장소를 거쳐가면, **방문 날짜가 담긴 지역별 특별 엽서와 아이템**을 제공합니다. 다양한 장소 방문으로 얻을 수 있는 희귀 엽서들과 특별 아이템은 **새로운 지역 탐방의 동기**가 됩니다. 여러 번 방문한 장소라도 문제 없습니다. **랜덤 시스템을 통해 새로운 수집의 즐거움**을 얻을 수 있습니다.
<br>
<br>
<br>



<div align="center">

### **3. 운전으로부터 만들어지는 나만의 공간 🏡**
</div>


<img width="5046" height="2420" alt="02-room-overview-tile" src="https://github.com/user-attachments/assets/bb5c198f-8236-42aa-b145-ba72f8b9ff4a" />

정체 구간을 지나고 여러 장소를 다니며 모은 코인으로 **섬과 집을 꾸며 보세요**. 상점에서 산 가구와 장식을 원하는 자리에 하나씩 놓다 보면 어느새 내 취향이 가득한 공간이 돼요. 마음에 드는 장면이 생기면 내 캐릭터를 세우고 포즈를 골라 **다양한 각도의 사진**을 남겨 보세요. 휴대폰으로 QR을 찍으면 **SNS에도 바로 공유**할 수 있어요.  

<br>
<br>

<div align="center">

### **4. 차량은 달라도 마음은 하나 ❤️**
</div>

<img width="10242" height="4912" alt="friends-new" src="https://github.com/user-attachments/assets/865e1a8d-0b9e-4887-9a66-86e6e57e7b31" />
 

소중한 사람들과 각자 다른 길을 달리면서도 함께 하고 싶을 때가 있죠. 친구가 운전 상태를 공개해 두면 친구가 지금 운전 중인지, 나와 같은 정체 구간을 지나고 있는지 알 수 있어요. 운전 중이 아닐 때에는 친구의 섬에 놀러 가 보세요. **친구가 꾸민 섬과 집을 구경하고, 방명록**도 남길 수 있어요. 친구 캐릭터를 꺼내 포즈를 정하고 그 옆에 내 캐릭터를 세우면 **둘이 함께 찍은 사진**이 완성돼요.

<br>

---------

<div align="center">
  
 ### 👇 **더 자세한 내용(QR을 통한 SNS 공유, 친구와 사진 찍기 등)은 아래 시연 영상에서 확인해 보세요!** 👇
 썸네일을 클릭하거나, 하단 배지를 클릭하면 이동합니다. 

  </div>

<a id="시연영상"></a>
<div align="center">
<a href="https://drive.google.com/file/d/12oYDrRN7IsIjh_xBsfFjy6mQaGaRZz9k/view?usp=sharing">
  <img width="1000" height="562" alt="th_unknown" src="https://github.com/user-attachments/assets/ec7599c8-5044-4016-b55f-cce52b738823" />
</a>


  
[![시연 영상 보기](https://img.shields.io/badge/시연_영상-002C5F?style=for-the-badge&logo=googledrive&logoColor=white)](https://drive.google.com/file/d/12oYDrRN7IsIjh_xBsfFjy6mQaGaRZz9k/view?usp=sharing)</div>


## 숏폼
<div align="center">
<a href="https://www.instagram.com/p/DfvFFu-yvK3/">
  <img width="1000" height="519" alt="thu_short_2" src="https://github.com/user-attachments/assets/c02efc43-21e3-4fc4-8301-da4eb268c3ef" />
</a>

[![숏폼 보기](https://img.shields.io/badge/숏폼(클릭)-E4405F?style=for-the-badge&logo=instagram&logoColor=white)](https://www.instagram.com/p/DfvFFu-yvK3/)
[![인스타그램 시연 영상 보기](https://img.shields.io/badge/시연_영상_Instagram_Ver(클릭)-002C5F?style=for-the-badge&logo=instagram&logoColor=white)](https://www.instagram.com/p/DfvDy0UyR5b/)

<br>
<br>

<p align="center">
  <img
    src="https://github.com/user-attachments/assets/1fe69b5c-eab6-486d-94af-fb541feeec1c"
    width="600">
</p>
<br>
<a id="team"></a>

### 🧑‍💻 만든 사람들: break가고장난8톤트럭 👩‍💻

[![정체불명 공식 인스타그램](https://img.shields.io/badge/공식_인스타그램_@jeongche.app-7F52FF?style=for-the-badge&logo=instagram&logoColor=white)](https://www.instagram.com/jeongche.app/)


<table>
  <tr>
    <td colspan="3" align="center">
      <a href="https://github.com/grassplatypus">
        <img src="https://github.com/grassplatypus.png" width="70" height="70" alt="grassplatypus"><br>
        grassplatypus
      </a><br>
      <sub>PM</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://github.com/comgongin">
        <img src="https://github.com/comgongin.png" width="70" height="70" alt="comgongin"><br>
        comgongin
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/kmxnix">
        <img src="https://github.com/kmxnix.png" width="70" height="70" alt="kmxnix"><br>
        kmxnix
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/samdi70">
        <img src="https://github.com/samdi70.png" width="70" height="70" alt="samdi70"><br>
        samdi70
      </a>
    </td>
  </tr>
</table>




  
