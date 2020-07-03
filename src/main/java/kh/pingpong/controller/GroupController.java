package kh.pingpong.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.JsonObject;

import kh.pingpong.dto.DeleteApplyDTO;
import kh.pingpong.dto.GroupApplyDTO;
import kh.pingpong.dto.GroupDTO;
import kh.pingpong.dto.GroupMemberDTO;
import kh.pingpong.dto.HobbyDTO;
import kh.pingpong.dto.JjimDTO;
import kh.pingpong.dto.LikeListDTO;
import kh.pingpong.dto.MemberDTO;
import kh.pingpong.dto.ReviewDTO;
import kh.pingpong.service.GroupService;

@Controller
@RequestMapping("/group/")
public class GroupController {
	@Autowired
	private GroupService gservice;
	
	@Autowired
	private HttpSession session;
	
	// header.jsp에서 그룹 찾기 탭 눌렀을 때 이동
	@RequestMapping("main")
	public String groupMain(String orderBy, HttpServletRequest request, Model model) throws Exception {
		int cpage = 1;
        try {
           cpage = Integer.parseInt(request.getParameter("cpage"));
        } catch (Exception e) {}
        
        Map<String, Object> search = new HashMap<>();
        
        search.put("orderBy", orderBy);
        
        List<HobbyDTO> hblist = gservice.selectHobby();
		List<GroupDTO> glist = gservice.selectGroupList(cpage, search);
		
		String navi = gservice.getPageNav(cpage, search);
		
		model.addAttribute("hblist", hblist);
		model.addAttribute("glist", glist);
		model.addAttribute("navi", navi);
		model.addAttribute("orderBy", orderBy);
		
		return "/group/main";
	}
	
	// 글 쓰기 페이지 이동
	@RequestMapping("write")
	public String groupWrite(Model model) {
		List<HobbyDTO> hblist = gservice.selectHobby();
		model.addAttribute("hblist", hblist);
		return "/group/write";
	}
	
	@RequestMapping("writeProc")
	public String groupWriteProc(GroupDTO gdto, Model model) throws ParseException {
		MemberDTO loginInfo = (MemberDTO)session.getAttribute("loginInfo");
		String id = loginInfo.getId();
		String name = loginInfo.getName();
		gdto.setWriter_id(id);
		gdto.setWriter_name(name);
		int seq = gservice.insertGroup(gdto);
		model.addAttribute("seq", seq);
		return "redirect:/group/view";
	}
	
	@RequestMapping("beforeView")
	public String groupBeforeView(int seq, Model model) {
		gservice.updateViewCount(seq);
		model.addAttribute("seq", seq);
		return "redirect:/group/view";
	}
	
	@Transactional("txManager")
	@RequestMapping("view")
	public String groupView(int seq, Model model) throws Exception{
		MemberDTO loginInfo = (MemberDTO)session.getAttribute("loginInfo");
		String id = loginInfo.getId();
		
		GroupDTO gdto = gservice.selectBySeq(seq);
		
		String hobby_type = gdto.getHobby_type();
		String hobby[] = hobby_type.split(",");
		List<String> hobby_arr = new ArrayList<>();
		
		for (int i = 0; i < hobby.length; i++) {
			hobby_arr.add(hobby[i]);
		}
		List<GroupDTO> relatedGroup = gservice.relatedGroup(hobby_arr);
		
		for (int i = 0; i < relatedGroup.size(); i++) {
			int rseq = relatedGroup.get(i).getSeq();
			
			if (rseq == seq) {
				relatedGroup.remove(i);
			}
		}
		
		if (relatedGroup.size() > 4) {
			relatedGroup.remove(4);
		}
		
		Map<Object, Object> param = new HashMap<>();
		param.put("id", id);
		param.put("parent_seq", seq);
		
		JjimDTO jdto = new JjimDTO();
		GroupApplyDTO gadto = new GroupApplyDTO();
		GroupMemberDTO gmdto = new GroupMemberDTO();
		
		jdto.setId(id);
		jdto.setParent_seq(seq);
		
		gadto.setId(id);
		gadto.setParent_seq(seq);
		
		
		gmdto.setId(id);
		gmdto.setParent_seq(seq);
		
		GroupMemberDTO checkGmdto = gservice.selectGroupMemberById(gmdto);
		
		boolean checkLike = gservice.selectLike(param);
		boolean checkJjim = gservice.selectJjim(jdto);
		boolean checkApply = gservice.selectApplyForm(gadto);
		boolean checkMember = true;
		if (checkGmdto == null) {
			checkMember = false;
		}
		
		//리뷰 리스트 출력
		List<ReviewDTO> reviewList = gservice.reviewList(seq);
		
		model.addAttribute("relatedGroup", relatedGroup);
		model.addAttribute("gdto", gdto);
		model.addAttribute("checkLike", checkLike);
		model.addAttribute("checkJjim", checkJjim);
		model.addAttribute("checkApply", checkApply);
		model.addAttribute("checkMember", checkMember);
		model.addAttribute("reviewList", reviewList);
		
		return "/group/view";
	}
	
	@RequestMapping("delete")
	public String groupDelete(int seq) {
		gservice.delete(seq);
		return "redirect:/group/main";
	}
	
	@RequestMapping("update")
	public String groupUpdate(int seq, Model model) throws Exception{
		List<HobbyDTO> hblist = gservice.selectHobby();
		GroupDTO gdto = gservice.selectBySeq(seq);
		model.addAttribute("hblist", hblist);
		model.addAttribute("gdto", gdto);
		return "/group/update";
	}
	
	@RequestMapping("updateProc")
	public String groupUpdateProc(GroupDTO gdto, Model model) throws ParseException {
		gservice.update(gdto);
		model.addAttribute("seq", gdto.getSeq());
		return "redirect:/group/view";
	}
	
	@RequestMapping("apply")
	public String groupApply(GroupApplyDTO gadto, Model model) {
		MemberDTO loginInfo = (MemberDTO)session.getAttribute("loginInfo");

		gadto.setId(loginInfo.getId());
		gadto.setName(loginInfo.getName());
		gadto.setAge(loginInfo.getAge());
		gadto.setGender(loginInfo.getGender());
		gadto.setAddress(loginInfo.getAddress());
		gadto.setProfile(loginInfo.getSysname());
		gadto.setLang_can(loginInfo.getLang_can());
		gadto.setLang_learn(loginInfo.getLang_learn());
		
		gservice.insertApp(gadto);
		
		int seq = gadto.getParent_seq();
		
		model.addAttribute("seq", seq);

		return "redirect:/group/view";
	}
	
	@RequestMapping("deleteApplyForm")
	public String deleteApplyForm(int seq, Model model) {
		MemberDTO loginInfo = (MemberDTO)session.getAttribute("loginInfo");
		String id = loginInfo.getId();
		
		GroupApplyDTO gadto = new GroupApplyDTO();
		
		gadto.setId(id);
		gadto.setParent_seq(seq);
		
		gservice.cancelApply(gadto);
		
		model.addAttribute("seq", seq);
		
		return "redirect:/group/view";
	}
	
	@RequestMapping("out")
	public String groupApplyDelete(DeleteApplyDTO dadto, Model model) {
		MemberDTO loginInfo = (MemberDTO)session.getAttribute("loginInfo");
		String id = loginInfo.getId();
		
		dadto.setId(id);
		gservice.insertDeleteApply(dadto);
		
		int seq = dadto.getParent_seq();
		
		model.addAttribute("seq", seq);
		
		return "redirect:/group/view";
	}
	
	@RequestMapping("like")
	@ResponseBody
	public int groupInsertLike(LikeListDTO ldto) {
		MemberDTO loginInfo = (MemberDTO)session.getAttribute("loginInfo");
		String id = loginInfo.getId();
		
		ldto.setId(id);
		
		int result = gservice.insertLike(ldto);
		return result;
	}
	
	@RequestMapping("jjim")
	@ResponseBody
	public int groupInsertJjim(JjimDTO jdto) {
		MemberDTO loginInfo = (MemberDTO)session.getAttribute("loginInfo");
		String id = loginInfo.getId();
		
		jdto.setId(id);
		
		int result = gservice.insertJjim(jdto);
		return result;
	}
	
	@RequestMapping("delJjim")
	@ResponseBody
	public int groupDeleteJjim(JjimDTO jdto) {
		MemberDTO loginInfo = (MemberDTO)session.getAttribute("loginInfo");
		String id = loginInfo.getId();
		
		jdto.setId(id);
		
		int result = gservice.deleteJjim(jdto);
		return result;
	}
	
	@RequestMapping("mainOption")
	public String mainOption(String orderBy, String ing, HttpServletRequest request, Model model) throws Exception {
		int cpage = 1;
        try {
           cpage = Integer.parseInt(request.getParameter("cpage"));
        } catch (Exception e) {}
        
        Map<String, Object> search = new HashMap<>();
        
        search.put("orderBy", orderBy);
        
        if (ing.contentEquals("done")) {
        	search.put("ing", "applying = 'N' and proceeding");
        	search.put("ingValue", "N");
        } else {
        	search.put("ing", ing);
        	search.put("ingValue", "Y");
        }
        
        List<HobbyDTO> hblist = gservice.selectHobby();
		List<GroupDTO> glist = gservice.selectGroupList(cpage, search);
		
		String navi = gservice.getPageNav(cpage, search);
		
		model.addAttribute("hblist", hblist);
		model.addAttribute("glist", glist);
		model.addAttribute("navi", navi);
		model.addAttribute("orderBy", orderBy);
		
		return "/group/main";
	}
	
	@RequestMapping("search")
	public String search(String orderBy, String keywordType, String keywordValue, String hobbyType, String period, HttpServletRequest request, Model model) throws Exception {
		int cpage = 1;
        try {
           cpage = Integer.parseInt(request.getParameter("cpage"));
        } catch (Exception e) {}
        
		Map<String, Object> search = new HashMap<>();
		
		if (!hobbyType.contentEquals("")) {
			String hobby[] = hobbyType.split(",");
			
			for (int i = 0; i < hobby.length; i++) {
				search.put("hobby_type" + i, hobby[i]);
			}
		}
		
		if (!keywordValue.contentEquals("null")) {
			search.put("keywordType", keywordType);
			search.put("keywordValue", keywordValue);
		}
		
		if (!period.contentEquals("null")) {
			search.put("period", period);
		}
		
		search.put("orderBy", orderBy);
		
		List<HobbyDTO> hblist = gservice.selectHobby();
		List<GroupDTO> glist = gservice.selectGroupList(cpage, search);
		
		String navi = gservice.getPageNav(cpage, search);

		model.addAttribute("hblist", hblist);
		model.addAttribute("glist", glist);
		model.addAttribute("navi", navi);
		model.addAttribute("orderBy", orderBy);
		
		return "/group/main";
	}
	
	@RequestMapping("searchDate")
	public String searchDate(String orderBy, String start_date, String end_date, HttpServletRequest request, Model model) throws Exception {
		int cpage = 1;
        try {
           cpage = Integer.parseInt(request.getParameter("cpage"));
        } catch (Exception e) {}
        
        Map<String, Object> search = new HashMap<>();
        
        search.put("orderBy", orderBy);
        search.put("start_date", start_date);
        search.put("end_date", end_date);
        
        List<HobbyDTO> hblist = gservice.selectHobby();
        List<GroupDTO> glist = gservice.selectGroupList(cpage, search);
        
        String navi = gservice.getPageNav(cpage, search);
        
        model.addAttribute("hblist", hblist);
        model.addAttribute("glist", glist);
        model.addAttribute("navi", navi);
        model.addAttribute("orderBy", orderBy);
        
        return "/group/main";
	}
	
	@RequestMapping("searchLocation")
	public String searchLocation(String location, String orderBy, HttpServletRequest request, Model model) throws Exception {
		int cpage = 1;
        try {
           cpage = Integer.parseInt(request.getParameter("cpage"));
        } catch (Exception e) {}

        Map<String, Object> search = new HashMap<>();
        
        search.put("orderBy", orderBy);
        search.put("location", location);
        
        List<HobbyDTO> hblist = gservice.selectHobby();
        List<GroupDTO> glist = gservice.selectGroupList(cpage, search);
        
        String navi = gservice.getPageNav(cpage, search);
        
        model.addAttribute("hblist", hblist);
        model.addAttribute("glist", glist);
        model.addAttribute("navi", navi);
        model.addAttribute("orderBy", orderBy);
        
        return "/group/main";
	}
	
	@RequestMapping(value="imgUpload", produces="application/json")
	@ResponseBody
	public JsonObject groupImgUpload(@RequestParam("file") MultipartFile multipartFile) {
		
		JsonObject jsonObject = new JsonObject();
		
		String fileRoot = "C:\\summernote_image\\";
		String originalFileName = multipartFile.getOriginalFilename();
		String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
		
		String savedFileName = UUID.randomUUID() + extension;
		
		File targetFile = new File(fileRoot + savedFileName);
		
		try {
			InputStream fileStream = multipartFile.getInputStream();
			FileUtils.copyInputStreamToFile(fileStream, targetFile);
			jsonObject.addProperty("url", "/summernoteImage/" + savedFileName);
			jsonObject.addProperty("responseCode", "success");
		} catch (IOException e) {
			FileUtils.deleteQuietly(targetFile);
			jsonObject.addProperty("responseCode", "error");
			e.printStackTrace();
		}
		
		return jsonObject;
	}
	
	// 리뷰 글쓰기 
	@ResponseBody
	@RequestMapping("reviewWrite")
	public String reviewWrite(ReviewDTO redto) throws Exception{
		int result = gservice.reviewWrite(redto);

		
		if(result > 0) { 
			 return String.valueOf(true); 
		}else { 
			return String.valueOf(false); 
		}
	}
}
