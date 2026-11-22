package io.github.disabilities;

import io.github.disabilities.config.ModConfig;
import net.minecraft.ChatFormatting;

public enum ConditionType {
	BLIND("Blind", "실명", ChatFormatting.DARK_GRAY,
		"Vision is obscured and sprinting is disabled",
		"시야가 가려지고 달리기를 할 수 없습니다"),
	COLOR_BLIND("Color Blind", "색맹", ChatFormatting.GRAY,
		"The world is rendered in monochrome grayscale",
		"세상이 흑백으로 보입니다"),
	DEAF("Deaf", "청각 장애", ChatFormatting.AQUA,
		"All in-game sound is silenced",
		"게임 내 모든 소리가 들리지 않습니다"),
	DIABETES("Diabetes", "당뇨병", ChatFormatting.LIGHT_PURPLE,
		"Sugary foods inflict damage proportional to sugar content",
		"당분이 높은 음식을 먹을수록 큰 피해를 입습니다"),
	SCHIZOPHRENIA("Schizophrenia", "조현병", ChatFormatting.DARK_PURPLE,
		"Phantom sounds and shadow figures appear around you",
		"실제로 존재하지 않는 소리와 그림자 환각이 나타납니다"),
	MUTE("Mute", "함묵증", ChatFormatting.WHITE,
		"Sending messages in chat is blocked",
		"채팅으로 말을 할 수 없습니다"),
	SEIZURES("Seizures", "발작", ChatFormatting.YELLOW,
		"Taking damage, starvation, or debuffs can trigger freezing seizures",
		"피격, 낮은 배고픔, 해로운 효과 시 확률적으로 발작이 발생합니다"),
	CANCER("Cancer", "암", ChatFormatting.DARK_GREEN,
		"Maximum health gradually deteriorates over time",
		"시간이 지날수록 최대 체력이 서서히 감소합니다"),
	OCD("OCD", "강박장애", ChatFormatting.BLUE,
		"Compulsive tasks must be completed accurately without overdoing them",
		"무작위 강박 과제를 정확히 수행해야 합니다 (초과 시 피해)"),
	AMPUTATED_OFFHAND("Amputated Offhand", "왼손 절단", ChatFormatting.GOLD,
		"Your offhand arm and slot cannot be used",
		"보조 손(오프핸드)을 사용할 수 없습니다"),
	AMPUTATED_LEG("Amputated Leg", "다리 절단", ChatFormatting.RED,
		"You cannot walk normally and must hop to move",
		"평소처럼 걸을 수 없고 점프해야만 이동할 수 있습니다"),
	DYSLEXIA("Dyslexia", "난독증", ChatFormatting.GREEN,
		"Most in-game text is scrambled and unreadable",
		"대부분의 게임 내 글자가 깨져 읽을 수 없습니다"),
	SHORT("Short", "저신장", ChatFormatting.DARK_AQUA,
		"Reduced height, shortened reach, and jump height limited to half a block",
		"키와 상호작용 사거리가 줄어들고 점프 높이가 반 블록으로 제한됩니다"),
	DEMENTIA("Dementia", "치매", ChatFormatting.DARK_BLUE,
		"Inventory items periodically shuffle and hide until clicked",
		"인벤토리 아이템이 무작위로 섞이고 클릭 전까지 숨겨집니다"),
	ASTHMA("Asthma", "천식", ChatFormatting.DARK_RED,
		"Extended sprinting drains oxygen and causes suffocation",
		"오래 달리면 호흡이 부족해져 질식 피해를 입습니다"),
	CEREBRAL_PALSY("Cerebral Palsy", "뇌성마비", ChatFormatting.GOLD,
		"Involuntary movements, jumps, and clicks happen randomly",
		"무작위 이동, 점프, 웅크리기, 클릭이 발생합니다"),
	NARCOLEPSY("Narcolepsy", "기면증", ChatFormatting.YELLOW,
		"You periodically black out and fall asleep",
		"무작위로 화면이 어두워지며 잠에 빠집니다"),
	PARKINSONS("Parkinson's", "파킨슨병", ChatFormatting.AQUA,
		"Your aim and crosshair tremble continuously",
		"조준점과 시야가 끊임없이 떨립니다"),
	POLLEN_ALLERGY("Pollen Allergy", "꽃가루 알레르기", ChatFormatting.GREEN,
		"Standing near flowers or pollen sources poisons you",
		"꽃이나 꽃가루 근처에 가면 독에 걸립니다"),
	INSOMNIA("Insomnia", "불면증", ChatFormatting.DARK_GRAY,
		"You are unable to sleep in beds",
		"침대에서 잠을 잘 수 없습니다"),
	ARTHRITIS("Arthritis", "관절염", ChatFormatting.GRAY,
		"Attack speed and mining speed are significantly reduced",
		"공격 속도와 채굴 속도가 크게 감소합니다"),
	BRITTLE_BONES("Brittle Bones", "골형성부전증", ChatFormatting.WHITE,
		"Fall damage is doubled and safe fall height is lowered",
		"낙하 피해가 2배가 되고 안전 낙하 높이가 줄어듭니다"),
	ADHD("ADHD", "ADHD", ChatFormatting.YELLOW,
		"Your selected hotbar slot switches at random",
		"선택한 핫바 슬롯이 무작위로 바뀝니다"),
	TOURETTES("Tourette's", "투렛 증후군", ChatFormatting.LIGHT_PURPLE,
		"Sudden camera twitches and involuntary chat outbursts",
		"카메라가 갑자기 튀고 채팅에 무작위 단어를 말합니다"),
	VERTIGO("Vertigo", "현기증", ChatFormatting.DARK_PURPLE,
		"Periodic bouts of nausea with a tilting, spinning view",
		"주기적으로 멀미와 함께 화면이 기울고 회전합니다"),
	PROSOPAGNOSIA("Prosopagnosia", "안면실인증", ChatFormatting.GOLD,
		"Every living mob looks identical",
		"모든 몹이 똑같은 모습으로 보입니다"),
	SCOLIOSIS("Scoliosis", "척추측만증", ChatFormatting.DARK_AQUA,
		"Your camera angle is permanently tilted",
		"시야가 영구적으로 기울어집니다"),
	KLEPTOMANIA("Kleptomania", "도벽", ChatFormatting.RED,
		"You refuse to drop or throw away items",
		"아이템을 버릴 수 없습니다"),
	LACTOSE_INTOLERANCE("Lactose Intolerance", "유당불내증", ChatFormatting.WHITE,
		"Consuming dairy products is instantly fatal",
		"우유가 들어간 음식을 먹으면 즉사합니다"),
	ANOSOGNOSIA("Anosognosia", "질병인식불능증", ChatFormatting.GRAY,
		"Your active conditions and alerts are hidden from you",
		"자신의 장애 목록과 알림을 볼 수 없습니다"),
	TETANUS("Tetanus", "파상풍", ChatFormatting.DARK_GREEN,
		"Holding any raw mineral or ore inflicts poison",
		"광물 원석이나 광석을 손에 들면 독에 걸립니다"),
	EATING_DISORDER("Eating Disorder", "섭식장애", ChatFormatting.RED,
		"Eating any food causes painful damage",
		"음식을 먹을 때마다 고통과 피해를 입습니다"),
	DIPLOPIA("Diplopia", "복시", ChatFormatting.LIGHT_PURPLE,
		"Your vision splits into two overlapping images",
		"화면이 두 개로 겹쳐 보입니다"),
	ASTIGMATISM("Astigmatism", "난시", ChatFormatting.AQUA,
		"Render distance is forced to 1 chunk, hiding everything far away",
		"청크 렌더 거리가 1로 강제 제한되어 멀리 있는 것이 보이지 않습니다"),
	MYOPIA("Myopia", "근시", ChatFormatting.BLUE,
		"Nearby objects turn pitch black, leaving only silhouettes",
		"가까이 있는 사물이 검게 가려져 실루엣만 보입니다"),
	HALLUCINATION("Hallucination", "환각", ChatFormatting.DARK_PURPLE,
		"Fake mobs appear around you and vanish when struck",
		"존재하지 않는 몹들이 보이며, 피해를 주지 않고 때리면 사라집니다"),
	GLAUCOMA("Glaucoma", "녹내장", ChatFormatting.DARK_GRAY,
		"Your peripheral vision closes in to a narrow tunnel",
		"주변 시야가 검게 좁아집니다"),
	CLAUSTROPHOBIA("Claustrophobia", "폐소공포증", ChatFormatting.DARK_RED,
		"Being in tight or enclosed indoor spaces deals continuous damage",
		"좁은 공간이나 사방이 막힌 곳(동굴/실내)에 있으면 지속 피해를 입습니다"),
	NYCTOPHOBIA("Nyctophobia", "어둠공포증", ChatFormatting.DARK_PURPLE,
		"Light level 4 or below inflicts darkness, slowness, and damage",
		"밝기가 4 이하인 어두운 곳에서 어둠, 이동속도 감소, 지속 피해를 입습니다"),
	AQUAPHOBIA("Aquaphobia", "물공포증", ChatFormatting.AQUA,
		"Touching water or standing in rain deals continuous damage",
		"물에 들어가거나 비를 맞으면 지속 피해를 입습니다"),
	PYROPHOBIA("Pyrophobia", "불공포증", ChatFormatting.GOLD,
		"Getting near lava, fire, torches, or campfires causes panic debuffs",
		"용암, 불, 횃불, 모닥불 근처에 가면 공황 디버프에 걸립니다"),
	AGORAPHOBIA("Agoraphobia", "광장공포증", ChatFormatting.YELLOW,
		"Staying in wide open outdoor spaces triggers panic debuffs",
		"탁 트인 야외에 오래 있으면 공황 디버프에 걸립니다"),
	ACROPHOBIA("Acrophobia", "고소공포증", ChatFormatting.WHITE,
		"Being at Y=65 or higher causes screen shaking, nausea, and slowness",
		"Y좌표 65 이상에 있으면 화면 떨림과 멀미, 구속에 걸립니다"),
	ARACHNOPHOBIA("Arachnophobia", "거미공포증", ChatFormatting.DARK_RED,
		"Getting near spiders or cave spiders causes screen shaking, weakness, and slowness",
		"거미나 동굴 거미 근처에 가면 화면 떨림과 나약함, 구속에 걸립니다");

	public final String displayName;
	public final String displayNameKo;
	public final ChatFormatting color;
	public final String description;
	public final String descriptionKo;

	ConditionType(String displayName, String displayNameKo, ChatFormatting color, String description, String descriptionKo) {
		this.displayName = displayName;
		this.displayNameKo = displayNameKo;
		this.color = color;
		this.description = description;
		this.descriptionKo = descriptionKo;
	}

	public String getDisplayName() {
		return ModConfig.isKorean() ? this.displayNameKo : this.displayName;
	}

	public String getDescription() {
		return ModConfig.isKorean() ? this.descriptionKo : this.description;
	}

	public static ConditionType parse(String input) {
		if (input == null) {
			return null;
		}
		String trimmed = input.trim();
		String normalized = trimmed.toUpperCase().replace('-', '_').replace(' ', '_');
		for (ConditionType type : values()) {
			if (type.name().equals(normalized)
				|| type.displayName.replace("'", "").replace(' ', '_').equalsIgnoreCase(normalized)
				|| type.displayNameKo.replace(" ", "").equalsIgnoreCase(trimmed.replace(" ", ""))) {
				return type;
			}
		}
		return null;
	}
}
